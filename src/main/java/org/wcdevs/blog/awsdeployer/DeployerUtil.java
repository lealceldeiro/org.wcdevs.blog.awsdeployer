package org.wcdevs.blog.awsdeployer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DeployerUtil {
  private DeployerUtil() {
  }

  static List<String> valuesFromCommaSeparatedString(String commaSeparatedString) {
    return valuesFromCommaSeparatedString(commaSeparatedString, "");
  }

  static List<String> valuesFromCommaSeparatedString(String commaSeparatedString,
                                                     String defaultIfNull) {
    return valuesFromCommaSeparatedString(commaSeparatedString, Function.identity(), defaultIfNull);
  }

  static <T> List<T> valuesFromCommaSeparatedString(String commaSeparatedString,
                                                    Function<String, T> mapper) {
    return valuesFromCommaSeparatedString(commaSeparatedString, mapper, "");
  }

  static <T> List<T> valuesFromCommaSeparatedString(String commaSeparatedString,
                                                    Function<String, T> mapper,
                                                    String defaultIfNull) {
    return Arrays.stream(Optional.ofNullable(commaSeparatedString)
                                 .orElse(defaultIfNull)
                                 .split(","))
                 .filter(Objects::nonNull)
                 .map(String::trim)
                 .filter(s -> !s.isEmpty())
                 .map(mapper)
                 .collect(Collectors.toList());
  }
}
