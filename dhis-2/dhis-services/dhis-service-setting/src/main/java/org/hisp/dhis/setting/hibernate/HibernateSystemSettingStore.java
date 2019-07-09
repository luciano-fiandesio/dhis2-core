package org.hisp.dhis.setting.hibernate;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hibernate.SessionFactory;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.*;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@Repository( "org.hisp.dhis.setting.SystemSettingStore" )
public class HibernateSystemSettingStore
    extends HibernateGenericStore<SystemSetting> implements SystemSettingStore
{
    @Autowired
    @Qualifier("readOnlyJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public HibernateSystemSettingStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate)
    {
        super( sessionFactory, jdbcTemplate, SystemSetting.class, true );
    }

    @Override
    //@Transactional( readOnly = true )
    public SystemSetting getByName( String name )
    {
        System.out.println(Thread.currentThread().getName());
        try {
//            try {
//            Thread.sleep((long)(Math.random() * 1500));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
            return jdbcTemplate.queryForObject( "SELECT VALUE FROM systemsetting where name = ?", new Object[] { name },
                ( rs, rowNum ) -> {
                    SystemSetting systemSetting = new SystemSetting();
                    systemSetting.setName( name );
                    byte[] val = rs.getBytes( "VALUE" );
                    try
                    {
                        systemSetting.setValue( deserialize( val ) );
                    }
                    catch ( IOException | ClassNotFoundException e )
                    {
                        e.printStackTrace();
                    }
                    return systemSetting;
                } );
        } catch (Exception e) {
            System.out.println("NAME: " + name + " ; " + e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public SystemSetting getByName2( String name )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
//        try {
//            Thread.sleep((long)(Math.random() * 2500));
//            System.out.println("waky waky: " + name);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List ll = sessionFactory.getCurrentSession().createNativeQuery("SELECT VALUE FROM systemsetting where name = '" + name +"'").getResultList();
        SystemSetting systemSetting = new SystemSetting();
        systemSetting.setName(name);
        try {
            byte[] val = (byte[]) ll.get(0);
            systemSetting.setValue(deserialize(val));
        } catch (Exception e) {
            //System.out.println("---- " + name);
            //e.printStackTrace();
        }
        return systemSetting;
//        return getSingleResult( builder, newJpaParameters()
//            .addPredicate( root -> builder.equal( root.get( "name" ), name ) ));
    }

    private Serializable deserialize(byte[] b) throws IOException, ClassNotFoundException {

        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return (Serializable) o;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }
}
