package hu.lanoga.toolbox.util;

import java.util.Collection;
import java.util.Map;

import org.springframework.util.StringUtils;

import hu.lanoga.toolbox.exception.ToolboxBadRequestException;
import hu.lanoga.toolbox.exception.ToolboxNotFoundException;

/**
 * Paraméter ellenőrzésekhez (nem tesztekbe! ott junit kell)... 
 * lényegében egyezik a {@link org.springframework.util.Assert} osztállyal (az ott deprecated default exception message üzenetek itt nem deprecated-ek) 
 */
public class ToolboxAssert extends org.springframework.util.Assert {

	private ToolboxAssert() {
		//
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalStateException}
	 * if the expression evaluates to {@code false}.
	 * <p>Call {@link #isTrue} if you wish to throw an {@code IllegalArgumentException}
	 * on an assertion failure.
	 * <pre class="code">Assert.state(id == null);</pre>
	 * @param expression a boolean expression
	 * @throws IllegalStateException if {@code expression} is {@code false}
	 */
	public static void state(final boolean expression) {
		state(expression, "[Assertion failed] - this state invariant must be true");
	}

	/**
	 * Assert a boolean expression, throwing an {@code IllegalArgumentException}
	 * if the expression evaluates to {@code false}.
	 * <pre class="code">Assert.isTrue(i &gt; 0);</pre>
	 * @param expression a boolean expression
	 * @throws IllegalArgumentException if {@code expression} is {@code false}
	 */
	public static void isTrue(final boolean expression) {
		isTrue(expression, "[Assertion failed] - this expression must be true");
	}
	
	/**
	 * Amenyiben nem igaz az expression, 
	 * akkor {@link ToolboxNotFoundException} exception dob (message lesz az ex. message)
	 * 
	 * @param expression
	 * @param message
	 */
	public static void isTrueNfe(final boolean expression, final String message) {
		if (!expression) {
			throw new ToolboxNotFoundException(message);
		}
	}
	
	/**
	 * Amenyiben nem igaz az expression, 
	 * akkor {@link ToolboxBadRequestException} exception dob (message lesz az ex. message)
	 * 
	 * @param expression
	 * @param message
	 */
	public static void isTrueBre(final boolean expression, final String message) {
		if (!expression) {
			throw new ToolboxBadRequestException(message);
		}
	}
	
	/**
	 * Assert that an object is {@code null}.
	 * <pre class="code">Assert.isNull(value);</pre>
	 * @param object the object to check
	 * @throws IllegalArgumentException if the object is not {@code null}
	 */
	public static void isNull(final Object object) {
		isNull(object, "[Assertion failed] - the object argument must be null");
	}

	/**
	 * Assert that an object is not {@code null}.
	 * <pre class="code">Assert.notNull(clazz);</pre>
	 * @param object the object to check
	 * @throws IllegalArgumentException if the object is {@code null}
	 */
	public static void notNull(final Object object) {
		notNull(object, "[Assertion failed] - this argument is required; it must not be null");
	}

	/**
	 * Assert that the given String is not empty; that is,
	 * it must not be {@code null} and not the empty String.
	 * <pre class="code">Assert.hasLength(name);</pre>
	 * @param text the String to check
	 * @see StringUtils#hasLength
	 * @throws IllegalArgumentException if the text is empty
	 */
	public static void hasLength(final String text) {
		hasLength(text,	"[Assertion failed] - this String argument must have length; it must not be null or empty");
	}
	
	/**
	 * Assert that the given String contains valid text content; that is, it must not
	 * be {@code null} and must contain at least one non-whitespace character.
	 * <pre class="code">Assert.hasText(name);</pre>
	 * @param text the String to check
	 * @see StringUtils#hasText
	 * @throws IllegalArgumentException if the text does not contain valid text content
	 */
	public static void hasText(final String text) {
		hasText(text, "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * <pre class="code">Assert.doesNotContain(name, "rod");</pre>
	 * @param textToSearch the text to search
	 * @param substring the substring to find within the text
	 * @throws IllegalArgumentException if the text contains the substring
	 */
	public static void doesNotContain(final String textToSearch, final String substring) {
		doesNotContain(textToSearch, substring,	"[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
	}

	/**
	 * Assert that an array contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">Assert.notEmpty(array);</pre>
	 * @param array the array to check
	 * @throws IllegalArgumentException if the object array is {@code null} or contains no elements
	 */
	public static void notEmpty(final Object[] array) {
		notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
	}

	/**
	 * Assert that an array contains no {@code null} elements.
	 * <p>Note: Does not complain if the array is empty!
	 * <pre class="code">Assert.noNullElements(array);</pre>
	 * @param array the array to check
	 * @throws IllegalArgumentException if the object array contains a {@code null} element
	 */
	public static void noNullElements(final Object[] array) {
		noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
	}

	/**
	 * Assert that a collection contains elements; that is, it must not be
	 * {@code null} and must contain at least one element.
	 * <pre class="code">Assert.notEmpty(collection);</pre>
	 * @param collection the collection to check
	 * @throws IllegalArgumentException if the collection is {@code null} or
	 * contains no elements
	 */
	public static void notEmpty(final Collection<?> collection) {
		notEmpty(collection, "[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
	}

	/**
	 * Assert that a Map contains entries; that is, it must not be {@code null}
	 * and must contain at least one entry.
	 * <pre class="code">Assert.notEmpty(map);</pre>
	 * @param map the map to check
	 * @throws IllegalArgumentException if the map is {@code null} or contains no entries
	 */
	public static void notEmpty(final Map<?, ?> map) {
		notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
	}
	
}
