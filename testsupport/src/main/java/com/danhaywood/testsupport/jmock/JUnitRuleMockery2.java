package com.danhaywood.testsupport.jmock;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.auto.internal.AllDeclaredFields;
import org.jmock.auto.internal.Mockomatic;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.picocontainer.DefaultPicoContainer;

/**
 * Use as a <tt>@Rule</tt>, meaning that the <tt>@RunWith(JMock.class)</tt> can
 * be ignored.
 * 
 * <pre>
 * public class MyTest {
 * 
 *     &#064;Rule
 *     public final Junit4Mockery2 context = Junit4Mockery2.createFor(Mode.INTERFACES);
 * 
 * }
 * </pre>
 * 
 * <p>
 * The class also adds some convenience methods, and uses a factory method to
 * make it explicit whether the context can mock only interfaces or interfaces
 * and classes.
 */
public class JUnitRuleMockery2 extends JUnit4Mockery implements MethodRule {

	
    /**
     * Factory method.
     */
    public static JUnitRuleMockery2 createFor(final Mode mode) {
        final JUnitRuleMockery2 jUnitRuleMockery2 = new JUnitRuleMockery2();
        if (mode == Mode.INTERFACES_AND_CLASSES) {
            jUnitRuleMockery2.setImposteriser(ClassImposteriser.INSTANCE);
        }
        return jUnitRuleMockery2;
    }

    
    @Retention(RUNTIME)
    @Target(FIELD)
	public static @interface ClassUnderTest {}

    public static enum Mode {
        INTERFACES_ONLY, INTERFACES_AND_CLASSES;
    }

	private final Mockomatic mockomatic = new Mockomatic(this);
	private final DefaultPicoContainer container = new DefaultPicoContainer();
	private Class<?> cutType;

    private JUnitRuleMockery2() {
    }

    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {

			@Override
            public void evaluate() throws Throwable {
                prepare(target);
                base.evaluate();
                assertIsSatisfied();
            }

            private void prepare(final Object target) {
                final List<Field> allFields = AllDeclaredFields.in(target.getClass());
                assertOnlyOneJMockContextIn(allFields);
                List<Object> mocks = fillInAutoMocks(target, allFields);
                cutType = locateClassUnderTestIfAny(allFields);
                if(cutType != null) {
	                for (Object mock : mocks) {
	                	container.addComponent(mock);
	                }
	                container.addComponent(cutType);
                }
            }

            private void assertOnlyOneJMockContextIn(final List<Field> allFields) {
                Field contextField = null;
                for (final Field field : allFields) {
                    if (JUnitRuleMockery2.class.isAssignableFrom(field.getType())) {
                        if (null != contextField) {
                            fail("Test class should only have one JUnitRuleMockery2 field, found " + contextField.getName() + " and " + field.getName());
                        }
                        contextField = field;
                    }
                }
            }

            private Class<?> locateClassUnderTestIfAny(final List<Field> allFields) {
                Field cutField = null;
                for (final Field field : allFields) {
                	if(field.getAnnotation(ClassUnderTest.class) != null) {
                        if (null != cutField) {
                            fail("Test class should only have one field annotated with @ClassUnderTest, found " + cutField.getName() + " and " + field.getName());
                        }
                        cutField = field;
                    }
                }
                return cutField != null? cutField.getType(): null;
            }

            private List<Object> fillInAutoMocks(final Object target, final List<Field> allFields) {
                return mockomatic.fillIn(target, allFields);
            }
        };
    }


    public Object getClassUnderTest() {
    	if(cutType == null) {
    		throw new IllegalStateException("No field annotated @ClassUnderTest was found");
    	}
    	return container.getComponent(cutType);
    }
    
    /**
     * Ignoring any interaction with the mock; an allowing/ignoring mock will be
     * returned in turn.
     */
    public <T> T ignoring(final T mock) {
        checking(new Expectations() {
            {
                ignoring(mock);
            }
        });
        return mock;
    }

    /**
     * Allow any interaction with the mock; an allowing mock will be returned in
     * turn.
     */
    public <T> T allowing(final T mock) {
        checking(new Expectations() {
            {
                allowing(mock);
            }
        });
        return mock;
    }

    /**
     * Prohibit any interaction with the mock.
     */
    public <T> T never(final T mock) {
        checking(new Expectations() {
            {
                never(mock);
            }
        });
        return mock;
    }

    /**
     * Ignore a set of mocks.
     */
    public void ignoring(Object... mocks) {
        for (Object mock : mocks) {
            ignoring(mock);
        }
    }
    
    
}