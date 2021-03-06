/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.xyz;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public abstract class Extensible<T> implements XyzSerializable {

  @JsonIgnore
  @JsonAnySetter
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonAnyGetter
  protected Map<String, Object> getAdditionalProperties() {
    if (this.additionalProperties == null) {
      this.additionalProperties = new HashMap<>();
    }
    return additionalProperties;
  }

  public <V> V get(Object key) {
    //noinspection unchecked
    return (V) this.getAdditionalProperties().get(key);
  }

  public void put(String key, Object value) {
    this.getAdditionalProperties().put(key, value);
  }

  public T with(String key, Object value) {
    put(key, value);
    return (T) this;
  }
}
