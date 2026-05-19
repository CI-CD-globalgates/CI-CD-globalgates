package com.app.globalgates.mybatis.handler;

import com.app.globalgates.common.enumeration.RagDocumentStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(RagDocumentStatus.class)
public class RagDocumentStatusHandler implements TypeHandler<RagDocumentStatus> {
    @Override
    public void setParameter(PreparedStatement ps, int i, RagDocumentStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public RagDocumentStatus getResult(ResultSet rs, String columnName) throws SQLException {
        return RagDocumentStatus.from(rs.getString(columnName));
    }

    @Override
    public RagDocumentStatus getResult(ResultSet rs, int columnIndex) throws SQLException {
        return RagDocumentStatus.from(rs.getString(columnIndex));
    }

    @Override
    public RagDocumentStatus getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return RagDocumentStatus.from(cs.getString(columnIndex));
    }
}
