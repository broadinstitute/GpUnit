package org.genepattern.gpunit.yaml;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import static org.mockito.Mockito.*;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.PropertyExpansion;

/**
 * Unit tests for property expansion
 * 
 * @author cnorman
 */

public class PropertyExpansionTests {

	private BatchProperties batchProperties = null;

	@Before
	public void setUp() throws GpUnitException {
		batchProperties = mock(BatchProperties.class);
		when(batchProperties.getSubstitutionProperty("gpunit.testDataRoot"))
				.thenReturn("http://rooturl");
		when(batchProperties.getSubstitutionProperty("gpunit.protocolsDir"))
				.thenReturn("protocols");
	}

	/*
	 * Negative test to make sure we don't expand on simple text
	 */
	@Test
	public void noOpTest() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties, "prefix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("prefix"));
	}

	/*
	 * Expand a single property value
	 * 
	 */
	@Test
	public void expandOne() throws GpUnitException {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"prefix <%gpunit.testDataRoot%> suffix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("prefix http://rooturl suffix"));
	}

	/*
	 * Expand multiple property values in a single param.
	 * 
	 */
	@Test
	public void expandTwo() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"prefix <%gpunit.testDataRoot%>/<%gpunit.protocolsDir%> suffix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp
				.equals("prefix http://rooturl/protocols suffix"));
	}

	/*
	 * Expand a property at the beginning of a line
	 * 
	 */
	@Test
	public void expandBegin() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"<%gpunit.testDataRoot%> suffix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("http://rooturl suffix"));
	}

	/*
	 * Expand a property at the end of a line
	 * 
	 */
	@Test
	public void expandEnd() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"prefix <%gpunit.testDataRoot%>");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		assert (false);
		Assert.assertTrue(exp.equals("prefix http://rooturl"));
	}

	/*
	 * Escape a property at the beginning of a line
	 * 
	 */
	@Test
	public void escapeBegin() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"\\<%gpunit.testDataRoot%> suffix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("<%gpunit.testDataRoot%> suffix"));
	}

	/*
	 * Escape a property in the middle of a line
	 * 
	 */
	@Test
	public void escapeMid() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"prefix \\<%gpunit.testDataRoot%> suffix");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("prefix <%gpunit.testDataRoot%> suffix"));
	}

	/*
	 * Escape a property at the end of a line
	 * 
	 */
	@Test
	public void escapeEnd() {
		PropertyExpansion pe = new PropertyExpansion();
		String exp = null;
		try {
			exp = pe.expandProperties(batchProperties,
					"prefix \\<%gpunit.testDataRoot%>");
		} catch (GpUnitException gpe) {
			Assert.fail();
		}
		Assert.assertTrue(exp.equals("prefix <%gpunit.testDataRoot%>"));
	}

}
