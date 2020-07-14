package org.acme.quotes;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeQuoteResourceIT extends QuoteResourceTest {

    // Execute the same tests but in native mode.
}