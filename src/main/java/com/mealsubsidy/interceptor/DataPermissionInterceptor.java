package com.mealsubsidy.interceptor;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.context.AuthContext;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.config.DataPermissionProperties;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门数据权限：员工仅本人，管理员仅本部门。菜品表 dish 不过滤。
 */
@Component
@RequiredArgsConstructor
public class DataPermissionInterceptor implements InnerInterceptor {

    private final DataPermissionProperties properties;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        if (!shouldFilter(ms)) {
            return;
        }
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(rewriteSql(mpBs.sql()));
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType type = ms.getSqlCommandType();
        if (type != SqlCommandType.UPDATE && type != SqlCommandType.DELETE) {
            return;
        }
        if (!shouldFilter(ms)) {
            return;
        }
        PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
        mpBs.sql(rewriteSql(mpBs.sql()));
    }

    private String rewriteSql(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                processSelectBody(((Select) statement).getSelectBody());
            } else if (statement instanceof Update) {
                Expression extra = buildCondition(((Update) statement).getTable());
                if (extra != null) {
                    ((Update) statement).setWhere(and(((Update) statement).getWhere(), extra));
                }
            } else if (statement instanceof Delete) {
                Expression extra = buildCondition(((Delete) statement).getTable());
                if (extra != null) {
                    ((Delete) statement).setWhere(and(((Delete) statement).getWhere(), extra));
                }
            }
            return statement.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("数据权限SQL解析失败", ex);
        }
    }

    private void processSelectBody(SelectBody selectBody) {
        if (selectBody instanceof PlainSelect) {
            appendPlainSelect((PlainSelect) selectBody);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList list = (SetOperationList) selectBody;
            if (list.getSelects() != null) {
                list.getSelects().forEach(this::processSelectBody);
            }
        }
    }

    private void appendPlainSelect(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof SubSelect sub && sub.getSelectBody() != null) {
            processSelectBody(sub.getSelectBody());
        }
        List<Expression> extras = new ArrayList<>();
        Expression fromCondition = buildCondition(fromItem);
        if (fromCondition != null) {
            extras.add(fromCondition);
        }
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                Expression joinCondition = buildCondition(join.getRightItem());
                if (joinCondition != null) {
                    extras.add(joinCondition);
                }
            }
        }
        Expression where = plainSelect.getWhere();
        for (Expression extra : extras) {
            where = and(where, extra);
        }
        plainSelect.setWhere(where);
    }

    private Expression buildCondition(FromItem fromItem) {
        if (!(fromItem instanceof Table table)) {
            return null;
        }
        return buildCondition(table);
    }

    private Expression buildCondition(Table table) {
        AuthUser user = AuthContext.get();
        if (user == null) {
            return null;
        }
        String tableName = unwrap(table.getName());
        if (!needFilter(tableName)) {
            return null;
        }
        String column = resolveColumn(tableName, user.getRole());
        if (!StringUtils.hasText(column)) {
            return null;
        }
        String alias = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(alias + "." + column));
        equalsTo.setRightExpression(new LongValue(resolveFilterValue(tableName, column, user)));
        return equalsTo;
    }

    private String resolveColumn(String tableName, String role) {
        if ("dept".equalsIgnoreCase(tableName)) {
            return "id";
        }
        if ("meal_audit".equalsIgnoreCase(tableName)) {
            return "dept_id";
        }
        if ("sys_user".equalsIgnoreCase(tableName)) {
            return RoleConstant.EMPLOYEE.equals(role) ? "id" : "dept_id";
        }
        return RoleConstant.EMPLOYEE.equals(role) ? "user_id" : "dept_id";
    }

    private Long resolveFilterValue(String tableName, String column, AuthUser user) {
        if ("user_id".equals(column) || ("id".equals(column) && "sys_user".equalsIgnoreCase(tableName))) {
            return user.getUserId();
        }
        return user.getDeptId();
    }

    private boolean needFilter(String tableName) {
        return properties.getTables().stream().anyMatch(t -> t.equalsIgnoreCase(tableName));
    }

    private String unwrap(String name) {
        return name == null ? "" : name.replace("`", "");
    }

    private Expression and(Expression left, Expression right) {
        if (right == null) {
            return left;
        }
        if (left == null) {
            return right;
        }
        return new AndExpression(left, new Parenthesis(right));
    }

    private boolean shouldFilter(MappedStatement ms) {
        if (!properties.isEnabled() || AuthContext.get() == null) {
            return false;
        }
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            return false;
        }
        return !hasIgnoreAnnotation(ms.getId());
    }

    private boolean hasIgnoreAnnotation(String mappedStatementId) {
        int split = mappedStatementId.lastIndexOf('.');
        if (split < 0) {
            return false;
        }
        try {
            Class<?> mapperClass = Class.forName(mappedStatementId.substring(0, split));
            if (AnnotationUtils.findAnnotation(mapperClass, IgnoreDataPermission.class) != null) {
                return true;
            }
            String methodName = mappedStatementId.substring(split + 1);
            for (Method method : mapperClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)
                        && AnnotationUtils.findAnnotation(method, IgnoreDataPermission.class) != null) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {
            return false;
        }
        return false;
    }
}
