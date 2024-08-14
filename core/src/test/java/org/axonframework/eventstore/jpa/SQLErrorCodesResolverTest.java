/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventstore.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.axonframework.common.AxonConfigurationException;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import jakarta.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Martin Tilma
 */
public class SQLErrorCodesResolverTest {

    @Test
    public void testIsDuplicateKey() throws Exception {
        SQLErrorCodesResolver sqlErrorCodesResolver = new SQLErrorCodesResolver(new ArrayList<Integer>());

        boolean isDuplicateKey = sqlErrorCodesResolver.isDuplicateKeyViolation(new PersistenceException("error",
                                                                                                        new RuntimeException()));

        assertFalse(isDuplicateKey);
    }

    @Test
    public void testIsDuplicateKey_isDuplicateKey_usingSetDuplicateKeyCodes() throws Exception {
        List<Integer> errorCodes = new ArrayList<Integer>();
        errorCodes.add(-104);
        SQLErrorCodesResolver sqlErrorCodesResolver = new SQLErrorCodesResolver(errorCodes);

        SQLException sqlException = new SQLException("test", "error", errorCodes.get(0));

        boolean isDuplicateKey = sqlErrorCodesResolver.isDuplicateKeyViolation(new PersistenceException("error",
                                                                                                        sqlException));

        assertTrue(isDuplicateKey);
    }


    @Test
    public void testIsDuplicateKey_isDuplicateKey_usingDataSource() throws Exception {
        String databaseProductName = "HSQL Database Engine";
        DataSource dataSource = createMockDataSource(databaseProductName);

        SQLErrorCodesResolver sqlErrorCodesResolver = new SQLErrorCodesResolver(dataSource);

        SQLException sqlException = new SQLException("test", "error", -104);

        boolean isDuplicateKey = sqlErrorCodesResolver.isDuplicateKeyViolation(new PersistenceException("error",
                                                                                                        sqlException));

        assertTrue(isDuplicateKey);
    }

    @Test
    public void testIsDuplicateKey_isDuplicateKey_usingProductName() throws Exception {
        String databaseProductName = "HSQL Database Engine";

        SQLErrorCodesResolver sqlErrorCodesResolver = new SQLErrorCodesResolver(databaseProductName);

        SQLException sqlException = new SQLException("test", "error", -104);

        boolean isDuplicateKey = sqlErrorCodesResolver.isDuplicateKeyViolation(new PersistenceException("error",
                                                                                                        sqlException));

        assertTrue(isDuplicateKey);
    }

    @Test
    public void testIsDuplicateKey_isDuplicateKey_usingCustomProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("MyCustom_Database_Engine.duplicateKeyCodes", "-104");

        String databaseProductName = "MyCustom Database Engine";
        DataSource dataSource = createMockDataSource(databaseProductName);

        SQLErrorCodesResolver sqlErrorCodesResolver = new SQLErrorCodesResolver(props, dataSource);

        SQLException sqlException = new SQLException("test", "error", -104);

        boolean isDuplicateKey = sqlErrorCodesResolver.isDuplicateKeyViolation(new PersistenceException("error",
                                                                                                        sqlException));

        assertTrue(isDuplicateKey);
    }

    @Test(expected = AxonConfigurationException.class)
    public void testInitialization_UnknownProductName() throws Exception {
        DataSource dataSource = createMockDataSource("Some weird unknown DB type");

        new SQLErrorCodesResolver(dataSource);
    }

    private DataSource createMockDataSource(String databaseProductName) throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData databaseMetaData = Mockito.mock(DatabaseMetaData.class);

        Mockito.when(databaseMetaData.getDatabaseProductName()).thenReturn(databaseProductName);
        Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
