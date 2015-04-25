package io.github.nalbion.maven.swagger;


import com.wordnik.swagger.models.Operation;
import com.wordnik.swagger.models.Response;
import com.wordnik.swagger.models.parameters.Parameter;
import com.wordnik.swagger.models.parameters.PathParameter;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class RestOperationTest {

    @Test
    public void should_support_multiple_path_parameters() {
        Operation op = Mockito.mock(Operation.class);
        Mockito.when(op.getVendorExtensions()).thenReturn(new HashMap<String, Object>());
        Mockito.when(op.getResponses()).thenReturn(new HashMap<String, Response>());

        List<Parameter> parameters = new ArrayList<>(3);
        PathParameter idParam = new PathParameter().name("id");
        PathParameter fooParam = new PathParameter().name("foo");
        PathParameter spamParam = new PathParameter().name("spam");
        idParam.setType("integer");
        fooParam.setType("number");
        fooParam.setFormat("double");
        spamParam.setType("string");
        parameters.add(idParam);
        parameters.add(fooParam);
        parameters.add(spamParam);
        Mockito.when(op.getParameters()).thenReturn(parameters);

        RestOperation rop1 = new RestOperation("GET", "/records/{id}/meta/{foo}/bar/{spam}", op);
        assertArrayEquals(new String[]{"id", "foo", "spam"}, rop1.getPathParameters());
        assertEquals("/records/(\\\\d+)/meta/(\\\\d+\\\\.\\\\d+)/bar/([^/]+)", rop1.getPathPattern());

        rop1 = new RestOperation("GET", "/records/{id}/meta/{foo}/bar", op);
        assertArrayEquals(new String[]{"id", "foo"}, rop1.getPathParameters());
        assertEquals("/records/(\\\\d+)/meta/(\\\\d+\\\\.\\\\d+)/bar", rop1.getPathPattern());

        rop1 = new RestOperation("GET", "/records/{id}/meta", op);
        assertArrayEquals(new String[]{"id"}, rop1.getPathParameters());
        assertEquals("/records/(\\\\d+)/meta", rop1.getPathPattern());

        rop1 = new RestOperation("GET", "/records/{id}", op);
        assertArrayEquals(new String[]{"id"}, rop1.getPathParameters());
        assertEquals("/records/(\\\\d+)", rop1.getPathPattern());

        rop1 = new RestOperation("GET", "/records/id", op);
        assertNull(rop1.getPathParameters());
        assertNull(rop1.getPathPattern());
    }
}
