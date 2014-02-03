//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.openejb.jndi;

import java.util.Map;

import javax.naming.Context;

import org.apache.openejb.OpenEJBRuntimeException;
import org.apache.openejb.SystemException;
import org.apache.openejb.core.JndiFactory;
import org.eclipse.jetty.jndi.NamingContext;
import org.eclipse.jetty.jndi.java.javaRootURLContext;
import org.eclipse.jetty.jndi.local.localContextRoot;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyJndiFactory implements JndiFactory
{
    private static final Logger LOG = Log.getLogger(JettyJndiFactory.class);
    private Context jndiRootContext;

    public JettyJndiFactory()
    {
        jndiRootContext = javaRootURLContext.getRoot();

        try
        {
            Context openEjbContext = jndiRootContext.createSubcontext("openejb");
            openEjbContext.createSubcontext("local");
            openEjbContext.createSubcontext("remote");
            openEjbContext.createSubcontext("client");
            openEjbContext.createSubcontext("Deployment");
            openEjbContext.createSubcontext("global");
            openEjbContext.createSubcontext("Container");
            openEjbContext.createSubcontext("Resource");
        }
        catch (javax.naming.NamingException e)
        {
            LOG.warn(e);
            throw new OpenEJBRuntimeException("Unable to create default OpenEJB entries",e);
        }
    }

    @Override
    public Context createComponentContext(Map<String, Object> bindings) throws SystemException
    {
        NamingContext context = localContextRoot.getRoot();

        for (Map.Entry<String, Object> entry : bindings.entrySet())
        {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value == null)
                continue;

            try
            {
                context.bind(name,value);
            }
            catch (javax.naming.NamingException e)
            {
                throw new org.apache.openejb.SystemException("Unable to bind '" + name + "' into bean's enc.",e);
            }
        }

        return context;
    }

    @Override
    public Context createRootContext()
    {
        return jndiRootContext;
    }
}