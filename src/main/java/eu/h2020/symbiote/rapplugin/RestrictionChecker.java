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
import eu.h2020.symbiote.rapplugin.value.PrimitiveValue;
import eu.h2020.symbiote.rapplugin.value.Value;
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

    public static boolean checkRestrictions(Value value, Restriction... restrictions) {
        return checkRestrictions(value, Stream.of(restrictions));
    }

    public static boolean checkRestrictions(Value value, List<Restriction> restrictions) {
        if (restrictions == null) {
            return true;
        }
        return checkRestrictions(value, restrictions.stream());
    }

    public static boolean checkRestrictions(Value value, Stream<Restriction> restrictions) {
        return restrictions.allMatch(x -> checkRestriction(value, x));
    }

    public static boolean checkRestriction(Value value, Restriction restriction) {
        if (!PrimitiveValue.class.isAssignableFrom(value.getClass())) {
            throw new RuntimeException("restrictions not allowed on complex values");
        }
        PrimitiveValue primitiveValue = (PrimitiveValue) value;
        if (RESTRICTIONS_TYPE_RESTRICTIONS.containsKey(restriction.getClass())) {
            if (!RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).isEmpty()
                    && !RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).stream()
                            .anyMatch(x -> x.getURI().equals(primitiveValue.getDatatype()))) {
                throw new RuntimeException("invalid datatype '" + primitiveValue.getDatatype() + "' for restriction type '" + restriction.getClass().getSimpleName() + "'. \r\n"
                        + "supported datatypes are "
                        + RESTRICTIONS_TYPE_RESTRICTIONS.get(restriction.getClass()).stream()
                                .map(x -> x.getURI())
                                .collect(Collectors.joining(", ", "[", "]")));
            }
        }
        if (StepRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkStepRestriction((StepRestriction) restriction, primitiveValue);
        } else if (RangeRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkRangeRestriction((RangeRestriction) restriction, primitiveValue);
        } else if (LengthRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkLengthRestriction((LengthRestriction) restriction, primitiveValue);
        } else if (EnumRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkEnumRestriction((EnumRestriction) restriction, primitiveValue);
        } else if (RegExRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkRegExRestriction((RegExRestriction) restriction, primitiveValue);
        } else if (InstanceOfRestriction.class.isAssignableFrom(restriction.getClass())) {
            return checkInstanceOfRestriction((InstanceOfRestriction) restriction, primitiveValue);
        }
        return false;
    }

    private static boolean checkRangeRestriction(RangeRestriction restriction, PrimitiveValue value) {
        return restriction.getMin() <= value.asNumber().doubleValue()
                && restriction.getMax() >= value.asNumber().doubleValue();
    }

    private static boolean checkStepRestriction(StepRestriction restriction, PrimitiveValue value) {
        return checkRangeRestriction((RangeRestriction) restriction, value)
                && (value.asNumber().doubleValue() % ((StepRestriction) restriction).getStep()) == 0;
    }

    private static boolean checkLengthRestriction(LengthRestriction restriction, PrimitiveValue value) {
        return restriction.getMin() <= value.asString().length()
                && restriction.getMax() >= value.asString().length();
    }

    private static boolean checkEnumRestriction(EnumRestriction restriction, PrimitiveValue value) {
        return restriction.getValues().contains(value.asString());
    }

    private static boolean checkInstanceOfRestriction(InstanceOfRestriction restriction, PrimitiveValue value) {
        return Utils.getPropertiesForClassFromPIM("", restriction.getInstanceOfClass(), restriction.getValueProperty()).stream()
                .anyMatch(x -> x.getDatatype().isValidValue(value.get())
                && Objects.equals(x.getValue(), value.get()));
    }

    private static boolean checkRegExRestriction(RegExRestriction restriction, PrimitiveValue value) {
        return value.asString().matches(restriction.getPattern());
    }

}
