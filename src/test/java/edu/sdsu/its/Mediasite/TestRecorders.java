package edu.sdsu.its.Mediasite;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class TestRecorders {
    private static final Logger LOGGER = Logger.getLogger(TestRecorders.class);

    @Test
    public void testGetAllRecorders() throws Exception {
        Recorders.Recorder[] recorders = Recorders.getRecorders();
        assertNotNull(recorders);
        for (Recorders.Recorder recorder : recorders) {
            LOGGER.debug(recorder.toString());
            assertNotNull(recorder.Id);
            assertNotNull(recorder.Name);
            // recorder.Description can be null
            assertNotNull(recorder.SerialNumber);
            assertNotNull(recorder.Version);
            assertNotNull(recorder.LastVersionUpdateDate);
            assertNotNull(recorder.PhysicalAddress);
            assertNotNull(recorder.ImageVersion);
        }
    }
}
