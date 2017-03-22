/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.test.util.jdbc.PreparedStatementProxyConnectionProvider;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
public class InsertOrderingWithBidirectionalOneToOne
		extends BaseNonConfigCoreFunctionalTestCase {
	private BatchCountingPreparedStatementObserver preparedStatementObserver = new BatchCountingPreparedStatementObserver();
	private PreparedStatementProxyConnectionProvider connectionProvider = new PreparedStatementProxyConnectionProvider(
			preparedStatementObserver
	);

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, Person.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void testBatching() throws SQLException {
		Session session = openSession();
		session.getTransaction().begin();
		{
			Person worker = new Person();
			Person homestay = new Person();

			Address home = new Address();
			Address office = new Address();

			home.addPerson( homestay );

			office.addPerson( worker );

			session.persist( home );
			session.persist( office );

			connectionProvider.clear();
		}
		session.getTransaction().commit();
		session.close();

		PreparedStatement addressPreparedStatement = preparedStatementObserver.getPreparedStatement(
				"insert into Address (ID) values (?)"
		);
		assertEquals( 2, preparedStatementObserver.getNumberOfBatchesAdded( addressPreparedStatement ) );

		PreparedStatement personPreparedStatement = preparedStatementObserver.getPreparedStatement(
				"insert into Person (address_ID, ID) values (?, ?)"
		);
		assertEquals( 2, preparedStatementObserver.getNumberOfBatchesAdded( personPreparedStatement ) );
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToOne(mappedBy = "address", cascade = CascadeType.PERSIST)
		private Person person;

		public void addPerson(Person person) {
			this.person = person;
			person.address = this;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToOne
		private Address address;
	}
}
