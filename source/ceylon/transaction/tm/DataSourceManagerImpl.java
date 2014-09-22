/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ceylon.transaction.tm;

import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.jdbc.TransactionalDriver;
import ceylon.transaction.tm.InitializationException;

import javax.naming.*;
import java.util.*;

/**
 * @author <a href="mailto:mmusgrov@redhat.com">Mike Musgrove</a>
 */
public class DataSourceManagerImpl {
    public void registerDriverSpec(String driverClassName, String moduleName, String moduleVersion,
                                   String dataSourceClassName) {
        XADSWrapperObjectFactory.registerDriverSpec(driverClassName, moduleName, moduleVersion, dataSourceClassName);
    }

    public void registerDataSource(String binding, String driver, String databaseUrl,
                                   String userName, String password) throws InitializationException {
        registerDataSource(binding, driver, databaseUrl, null, -1, userName, password);
    }

    public void registerDataSource(String binding, String driver, String databaseName,
                                   String host, long port, String userName,
                                   String password) throws InitializationException {
        XADSWrapper xaDataSourceToBind;
        Properties p = System.getProperties();
        String driversProp = (String) p.get("jdbc.drivers");

        if (driversProp == null)
            p.put("jdbc.drivers", driver);
        else
            p.put("jdbc.drivers", driversProp + ":" + driver);

        p.put(TransactionalDriver.userName, userName);
        p.put(TransactionalDriver.password, password);

        System.setProperties(p);

        try {
            xaDataSourceToBind = XADSWrapperObjectFactory.getXADataSource(
                binding, driver, databaseName, host, (int) port, userName, password);
        } catch (Exception e) {
            if (tsLogger.logger.isInfoEnabled())
                tsLogger.logger.info("Cannot bind " + databaseName + " for driver " + driver);

            throw new InitializationException("Cannot bind " + databaseName + " for driver " + driver, e);
        }

        try {
            new InitialContext().rebind(binding, xaDataSourceToBind);
        } catch (Exception e) {
            if (tsLogger.logger.isInfoEnabled())
                tsLogger.logger.infof("%s: Cannot bind datasource into JNDI: %s", binding, e.getMessage());

            throw new InitializationException(binding + ": Cannot bind datasource into JNDI", e);
        }

        TransactionServiceFactory.registerJDBCXARecoveryHelper(binding, userName, password);
    }
}