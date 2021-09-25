package org.wcdevs.blog.awsdeployer;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class UtilTest {
  private static String randomString() {
    return UUID.randomUUID().toString();
  }

  @Test
  void getValueInAppThrowsNPEIfKeyIsNull() {
    assertThrows(NullPointerException.class, () -> Util.getValueInApp(null, mock(App.class)));
  }

  @Test
  void getValueInAppThrowsNPEIfAppIsNull() {
    assertThrows(NullPointerException.class, () -> Util.getValueInApp(randomString(), null));
  }

  @Test
  void environmentFromNPEAccountId() {
    assertThrows(NullPointerException.class, () -> Util.environmentFrom(null, randomString()));
  }

  @Test
  void environmentFromNPERegion() {
    assertThrows(NullPointerException.class, () -> Util.environmentFrom(randomString(), null));
  }

  @Test
  void environmentFrom() {
    try (MockedStatic<Environment> mockedEnvironment = mockStatic(Environment.class)) {
      Environment.Builder envBuilderMock = mock(Environment.Builder.class);
      when(envBuilderMock.account(anyString())).thenReturn(envBuilderMock);
      when(envBuilderMock.region(anyString())).thenReturn(envBuilderMock);
      Environment expected = mock(Environment.class);
      when(envBuilderMock.build()).thenReturn(expected);

      mockedEnvironment.when(Environment::builder).thenReturn(envBuilderMock);

      assertEquals(expected, Util.environmentFrom(randomString(), randomString()));
    }
  }

  @Test
  void string() {
    String s1 = randomString(), s2 = randomString();
    assertEquals(s1 + s2, Util.string(s1, s2));
  }

  @Test
  void stringFromNull() {
    assertEquals("", Util.string(null, null, null));
  }

  @Test
  void joinedString() {
    String s1 = randomString(), s2 = randomString(), joiner = randomString();
    assertEquals(s1 + joiner + s2, Util.joinedString(joiner, s1, s2));
  }
}
