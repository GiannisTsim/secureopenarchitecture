package mil.navy.spawar.soaf.security.filters;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;

import mil.navy.spawar.soaf.security.SoafUserDetailsImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.BasicDBObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:sci-module-test-config.xml"})
public class SCIMongoQueryFilterTests {

	private static final String EXPECTED_DB_LABEL = "securityLabel";
	private static final String EXPECTED_DB_ATTR = "SCI";
	private static final String EXPECTED_USERDETAIL_ATTR = "sciMarking";
	
	private static final String WHERE = "$where";

	@Autowired(required=true)
	SCIMongoQueryFilter filter;
	
	@Autowired(required=true)
	SoafUserDetailsImpl userDetails;
	
	private Map<String, Collection<String>> userAttrs;

	@Before
	public void setUp() {
		
		assertNotNull(filter);
		assertTrue(filter instanceof SCIMongoQueryFilter);
		assertNotNull(filter.getAttributeConfig());
		assertEquals(EXPECTED_DB_LABEL, filter.getDatabaseLabel());
		assertEquals(EXPECTED_DB_ATTR, filter.getAttributeConfig().getDbAttributeName());
		assertEquals(EXPECTED_USERDETAIL_ATTR, filter.getAttributeConfig().getUserDetailsName());
		

		assertNotNull(userDetails);
		assertTrue(userDetails instanceof SoafUserDetailsImpl);
		assertTrue(userDetails.getCustomAttributes().containsKey(EXPECTED_USERDETAIL_ATTR));
		assertEquals(3,userDetails.getCustomAttributes().get(EXPECTED_USERDETAIL_ATTR).size());

		userAttrs = userDetails.getCustomAttributes();
	}

	@Test
	public void testUserHasSciMarkings()  {

		int kount = userAttrs.get(EXPECTED_USERDETAIL_ATTR).size();
		String[] userAttrList = userAttrs.get(EXPECTED_USERDETAIL_ATTR).toArray(new String[kount]);
		
		// do filter
		BasicDBObject filterResult = filter.filter(userDetails);

		// check post-conditions
		assertNotNull(filterResult);
		assertNotNull(filterResult.get(WHERE));
		assertTrue(filterResult.get(WHERE) instanceof String);
		
		String actualResult = (String) filterResult.get(WHERE);
		StringBuilder expectedResut = new StringBuilder("satisfies([");
		for(int i=0;i<userAttrList.length; i++) {
			expectedResut.append("'" + userAttrList[i] + "'");
			if( i<userAttrList.length-1) {
				expectedResut.append(",");
			}
		}
		expectedResut.append("],this." + EXPECTED_DB_LABEL + "." + EXPECTED_DB_ATTR + ")");
		assertEquals(expectedResut.toString(), actualResult);
	}
	
	@Test
	@DirtiesContext
	public void testUserHasNoSciMarkings()  {

		// make user have no access
		userAttrs.get(EXPECTED_USERDETAIL_ATTR).clear();
		assertEquals(0,userAttrs.get(EXPECTED_USERDETAIL_ATTR).size());
		
		// do filter
		BasicDBObject filterResult = filter.filter(userDetails);

		// check post-conditions
		assertNotNull(filterResult);
		assertNotNull(filterResult.get(WHERE));
		assertTrue(filterResult.get(WHERE) instanceof String);
		
		String actualResult = (String) filterResult.get(WHERE);
		StringBuilder expectedResut = new StringBuilder("satisfies([],this." + EXPECTED_DB_LABEL + "." + EXPECTED_DB_ATTR + ")");

		assertEquals(expectedResut.toString(), actualResult);
	}	
	
	@Test
	@DirtiesContext
	public void testUserHasNoSciAttribute()  {
		
		// make user have no attr
		userAttrs.clear();
		assertFalse(userAttrs.containsKey(EXPECTED_USERDETAIL_ATTR));
		
		// do filter
		BasicDBObject filterResult = filter.filter(userDetails);

		// check post-conditions
		assertNotNull(filterResult);
		assertNotNull(filterResult.get(WHERE));
		assertTrue(filterResult.get(WHERE) instanceof String);

		String actualResult = (String) filterResult.get(WHERE);
		StringBuilder expectedResut = new StringBuilder("satisfies([],this." + EXPECTED_DB_LABEL + "." + EXPECTED_DB_ATTR + ")");

		assertEquals(expectedResut.toString(), actualResult);
	}		
}
