package co.kenrg.mega.repl.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import co.kenrg.mega.repl.evaluator.Environment.SetBindingStatus;
import co.kenrg.mega.repl.object.IntegerObj;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

    @Test
    public void testGet_noBindingForName_returnsNull() {
        Environment env = new Environment();
        assertNull(env.get("missingBinding"));
    }

    @Test
    public void testGet_bindingAdded_returnsBinding() {
        Environment env = new Environment();
        env.add("binding", new IntegerObj(1), true);
        assertEquals(new IntegerObj(1), env.get("binding"));
    }

    @Test
    public void testGet_bindingAddedToParent_returnsParentBinding() {
        Environment parent = new Environment();
        parent.add("binding", new IntegerObj(1), true);

        Environment child = parent.createChildEnvironment();
        assertEquals(new IntegerObj(1), child.get("binding"));
    }

    @Test
    public void testAdd_bindingAlreadyAddedToEnv_returnsError() {
        Environment env = new Environment();
        assertEquals(SetBindingStatus.NO_ERROR, env.add("binding", new IntegerObj(1), true));
        assertEquals(SetBindingStatus.E_DUPLICATE, env.add("binding", new IntegerObj(2), true));
    }
}