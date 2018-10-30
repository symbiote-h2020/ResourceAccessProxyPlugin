/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import eu.h2020.symbiote.model.cim.EnumRestriction;
import eu.h2020.symbiote.model.cim.InstanceOfRestriction;
import eu.h2020.symbiote.model.cim.LengthRestriction;
import eu.h2020.symbiote.model.cim.RangeRestriction;
import eu.h2020.symbiote.model.cim.RegExRestriction;
import eu.h2020.symbiote.model.cim.Restriction;
import eu.h2020.symbiote.model.cim.StepRestriction;
import eu.h2020.symbiote.rapplugin.util.Utils;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class RestrictionChecker {

    private static final List<RDFDatatype> STRING_TYPES = Arrays.asList(
            XSDDatatype.XSDstring,
            RDFLangString.rdfLangString);

    private static final List<RDFDatatype> NUMERIC_TYPES = Arrays.asList(
            XSDDatatype.XSDinteger,
            XSDDatatype.XSDint,
            XSDDatatype.XSDdecimal,
            XSDDatatype.XSDdouble,
            XSDDatatype.XSDbyte,
            XSDDatatype.XSDfloat,
            XSDDatatype.XSDlong,
            XSDDatatype.XSDbyte,
            XSDDatatype.XSDnegativeInteger,
            XSDDatatype.XSDnonNegativeInteger,
            XSDDatatype.XSDnonPositiveInteger,
            XSDDatatype.XSDpositiveInteger,
            XSDDatatype.XSDshort,
            XSDDatatype.XSDunsignedByte,
            XSDDatatype.XSDunsignedInt,
            XSDDatatype.XSDunsignedLong,
            XSDDatatype.XSDunsignedShort);

    private static final Map<Class<?>, List<RDFDatatype>> RESTRICTIONS_TYPE_RESTRICTIONS = initAllowedTypes();

    private static Map<Class<?>, List<RDFDatatype>> initAllowedTypes() {
        Map<Class<?>, List<RDFDatatype>> result = new HashMap<>();
        result.put(RangeRestriction.class, NUMERIC_TYPES);
        result.put(StepRestriction.class, NUMERIC_TYPES);
        result.put(LengthRestriction.class, STRING_TYPES);
        result.put(RegExRestriction.class, STRING_TYPES);
        result.put(EnumRestriction.class, STRING_TYPES);
        result.put(InstanceOfRestriction.class, Arrays.asList());
        return result;
    }

    private RestrictionChecker() {
    }

    public static void checkRestrictions(Object value, RDFDatatype rdfDatatype, Restriction... restrictions) throws ValidationException {
        checkRestrictions(value, rdfDatatype, Stream.of(restrictions));
    }

    public static void checkRestrictions(Object value, RDFDatatype rdfDatatype, List<Restriction> restrictions) throws ValidationException {
        if (restrictions != null) {
            for (Restriction restriction : restrictions) {
                checkRestriction(value, rdfDatatype, restriction);
            }
        }
    }

    public static void checkRestrictions(Object value, RDFDatatype rdfDatatype, Stream<Restriction> restrictions) throws ValidationException {
        if (restrictions != null) {
            checkRestrictions(value, rdfDatatype, restrictions.collect(Collectors.toList()));
        }
    }

    public static void checkRestriction(Object value, RDFDatatype rdfDatatype, Restriction restriction) throws ValidationException {
        if (RESTRICTIONS_TYPE_RESTRICTIONS.containsKey(restriction.getClass())) {
            if (!RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).isEmpty()
                    && !RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).stream()
                            .anyMatch(x -> x.getURI().equals(rdfDatatype))) {
                throw new RuntimeException("invalid datatype '" + rdfDatatype + "' for restriction type '" + restriction.getClass().getSimpleName() + "'. \r\n"
                        + "supported datatypes are "
                        + RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).stream()
                                .map(x -> x.getURI())
                                .collect(Collectors.joining(", ", "[", "]")));
            }
        }
        if (StepRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkStepRestriction((StepRestriction) restriction, value);
        } else if (RangeRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkRangeRestriction((RangeRestriction) restriction, value);
        } else if (LengthRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkLengthRestriction((LengthRestriction) restriction, value);
        } else if (EnumRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkEnumRestriction((EnumRestriction) restriction, value);
        } else if (RegExRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkRegExRestriction((RegExRestriction) restriction, value);
        } else if (InstanceOfRestriction.class.isAssignableFrom(restriction.getClass())) {
            checkInstanceOfRestriction((InstanceOfRestriction) restriction, value);
        }
    }

    private static Number asNumber(Object value) throws ValidationException {
        try {
            return NumberFormat.getInstance().parse(value.toString());
        } catch (ParseException ex) {
            throw new ValidationException("value '" + value + "' cannot be converted to datatype '" + Number.class.getSimpleName() + "'", ex);
        }
    }

    private static String asString(Object value) throws ValidationException {
        return value.toString();
    }

    private static void checkRangeRestriction(RangeRestriction restriction, Object value) throws ValidationException {
        if (!(restriction.getMin() <= asNumber(value).doubleValue()
                && restriction.getMax() >= asNumber(value).doubleValue())) {
            throw new ValidationException("value must be numeric, >= " + restriction.getMin() + " and <= " + restriction.getMax());
        }
    }

    private static void checkStepRestriction(StepRestriction restriction, Object value) throws ValidationException {
        checkRangeRestriction((RangeRestriction) restriction, value);
        if (!((asNumber(value).doubleValue() % ((StepRestriction) restriction).getStep()) == 0)) {
            throw new ValidationException("value must multiple of " + ((StepRestriction) restriction).getStep());
        }
    }

    private static void checkLengthRestriction(LengthRestriction restriction, Object value) throws ValidationException {
        if (!(restriction.getMin() <= asString(value).length()
                && restriction.getMax() >= asString(value).length())) {
            throw new ValidationException("string length must be >= " + restriction.getMin() + " and >= " + restriction.getMax());
        }
    }

    private static void checkEnumRestriction(EnumRestriction restriction, Object value) throws ValidationException {
        if (!restriction.getValues().contains(asString(value))) {
            throw new ValidationException("value '" + value + "' not allowed, must be one of [" + String.join(", ", restriction.getValues()) + "]");
        }
    }

    private static void checkInstanceOfRestriction(InstanceOfRestriction restriction, Object value) throws ValidationException {
        List<Literal> allowedValues = Utils.getPropertiesForClassFromPIM("", restriction.getInstanceOfClass(), restriction.getValueProperty());
        if (!allowedValues.stream().anyMatch(x -> x.getDatatype().isValidValue(value) && Objects.equals(x.getValue(), value))) {
            throw new ValidationException(
                    "value '" + value + "' must be property '" + restriction.getValueProperty() + "' of an instance of class '" + restriction.getInstanceOfClass() + "' in PIM. "
                    + "Allowed values are [" + allowedValues.stream().map(x -> x.getValue().toString()).collect(Collectors.joining(", ")) + "]");
        }

    }

    private static void checkRegExRestriction(RegExRestriction restriction, Object value) throws ValidationException {
        if (!asString(value).matches(restriction.getPattern())) {
            throw new ValidationException("value '" + value + "' does not match regex pattern '" + restriction.getPattern() + "'");
        }
    }

}
