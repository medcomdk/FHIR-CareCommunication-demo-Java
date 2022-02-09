package com.trifork.receiver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ResourceParser {




  static <T> T getSingleResource(Class clazz, List<IBaseResource> resourceList) {
    return (T) resourceList.stream().filter(clazz::isInstance).map(clazz::cast)
        .collect(singleElement()).get();
  }

  static List getResources(Class clazz, List<IBaseResource> resourceList) {
    return resourceList.stream().filter(clazz::isInstance).map(clazz::cast)
        .collect(Collectors.toList());
  }

  static <T> Collector<T, ?, Optional<T>> singleElement() {
    return Collectors.collectingAndThen(Collectors.toList(),
        list -> list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty());
  }


}
