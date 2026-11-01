package org.unitseek.knitid.sequence.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JDBC sequence configuration.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "knitid.sequence.jdbc")
public class JdbcSequenceProperties {

    private boolean enabled = true;
    private String tableName = "knitid_sequence";
    private int blockSize = 100;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("knitid.sequence.jdbc.table-name must be a simple SQL identifier");
        }
        this.tableName = tableName;
    }
    public int getBlockSize() { return blockSize; }
    public void setBlockSize(int blockSize) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("knitid.sequence.jdbc.block-size must be > 0");
        }
        this.blockSize = blockSize;
    }
}
