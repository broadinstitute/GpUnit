package org.genepattern.gpunit;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.BatchProperties;

/**
 * Match and resolve property references embedded in yaml of the form:
 *  
 *   "property <property_name>"
 *    
 * The "<" token can be escaped with a backslash:
 * 
 *    "escaped property reference \<property_name>"
 *
 * but this is not necessary (and should not be done) unless the "<" is followed by a ">" with an
 * intervening string that looks like a property name, as in the example above.
 * 	
 * @author cnorman
 */

public class PropertyExpansion {
	private Pattern pattern = null;

	private Pattern getCompiledPattern() throws GpUnitException {
		// Note: we match an entire property reference with an optional leading backslash.
		// The parens in the regexp are purely for the capture group that represents the actual property.
		String regExp = "\\\\*<\\%([a-zA-Z0-9._]+)\\%>";
		if (null == pattern) {
			try {
				pattern = Pattern.compile(regExp);
			} catch (PatternSyntaxException pe) {
				throw new GpUnitException(
						"Error compiling pattern for yaml file property substitution: "
								+ pe.toString());
			}
		}

		return pattern;
	}

	/*
	 * Resolve all embedded property references to a property in BatchProperties.
	 * 
	 */
	public String expandProperties(BatchProperties bp, String originalStr)
			throws GpUnitException {
		Matcher match = getCompiledPattern().matcher(originalStr);
		StringBuffer sb = new StringBuffer(originalStr.length());
		try {
			while (match.find()) {
				String fullMatch = match.group(0);
				if (!fullMatch.startsWith("\\")) {
					String propName = match.group(1);
					String newValue = bp.getSubstitutionProperty(propName);
					String quoted = Matcher.quoteReplacement(newValue);
					match.appendReplacement(sb, quoted);
				}
				else {
					String stripEscape = fullMatch.substring(1, fullMatch.length());
					match.appendReplacement(sb, stripEscape);
				}
			}
		} catch (Exception pe) {
			throw new GpUnitException(
					"Error in yaml file property substitution: "
							+ pe.toString());
		}

		match.appendTail(sb);	// capture the remainder of the original string
		return sb.toString();
	}
}
