package com.app.globalgates.mybatis.handler;

import com.app.globalgates.common.enumeration.RagProcessStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(RagProcessStatus.class)
public class RagProcessStatusHandler implements TypeHandler<RagProcessStatus> {
    @Override
    public void setParameter(PreparedStatement ps, int i, RagProcessStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public RagProcessStatus getResult(ResultSet rs, String columnName) throws SQLException {
        return RagProcessStatus.from(rs.getString(columnName));
    }

    @Override
    public RagProcessStatus getResult(ResultSet rs, int columnIndex) throws SQLException {
        return RagProcessStatus.from(rs.getString(columnIndex));
    }

    @Override
    public RagProcessStatus getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return RagProcessStatus.from(cs.getString(columnIndex));
    }
}
