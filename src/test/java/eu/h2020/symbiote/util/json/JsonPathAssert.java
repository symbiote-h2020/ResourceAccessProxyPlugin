package eu.h2020.symbiote.util.json;

import com.jayway.jsonpath.DocumentContext;
import org.assertj.core.api.*;

/**
 * Assertions for {@link DocumentContext}.
 *
 * @author Jorge Lee
 */
public class JsonPathAssert extends AbstractAssert<JsonPathAssert, DocumentContext> {

    public JsonPathAssert(DocumentContext actual) {
        super(actual, JsonPathAssert.class);
    }

    public static JsonPathAssert assertThat(DocumentContext documentContext) {
        return new JsonPathAssert(documentContext);
    }

    /**
     * Extracts a JSON text using a JsonPath expression and wrap it in a {@link StringAssert}.
     *
     * @param path JsonPath to extract the string
     * @return an instance of {@link StringAssert}
     */
    public AbstractCharSequenceAssert<?, String> jsonPathAsString(String path) {
        return Assertions.assertThat(actual.read(path, String.class));
    }

    /**
     * Extracts a JSON number using a JsonPath expression and wrap it in an {@link IntegerAssert}
     *
     * @param path JsonPath to extract the number
     * @return an instance of {@link IntegerAssert}
     */
    public AbstractIntegerAssert<?> jsonPathAsInteger(String path) {
        return Assertions.assertThat(actual.read(path, Integer.class));
    }

    public AbstractBooleanAssert<?> jsonPathAsBoolean(String path) {
        return Assertions.assertThat(actual.read(path, Boolean.class));
    }
    
    
    public AbstractObjectAssert<?, Object> jsonPath(String path) {
        return Assertions.assertThat(actual.read(path, Object.class));
    }
}
