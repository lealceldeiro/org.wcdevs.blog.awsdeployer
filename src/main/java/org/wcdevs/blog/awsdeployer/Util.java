package org.wcdevs.blog.awsdeployer;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

final class Util {
  private Util() {
  }

  static <T> T getValueInApp(String valueKey, App app) {
    return getValueInApp(valueKey, app, true);
  }

  @SuppressWarnings("unchecked")
  static <T> T getValueInApp(String valueKey, App app, boolean notNull) {
    T value = (T) Objects.requireNonNull(app)
                         .getNode()
                         .tryGetContext(Objects.requireNonNull(valueKey));
    return notNull
           ? Objects.requireNonNull(value, String.format("'%s' cannot be null", valueKey))
           : value;
  }

  static Environment environmentFrom(String accountId, String region) {
    return Environment.builder()
                      .account(Objects.requireNonNull(accountId))
                      .region(Objects.requireNonNull(region))
                      .build();
  }

  static String string(Object... values) {
    return string("", values);
  }

  static String string(String joiner, Object... values) {
    return Arrays.stream(values)
                 .filter(Objects::nonNull)
                 .map(Object::toString)
                 .collect(Collectors.joining(joiner));
  }
}
