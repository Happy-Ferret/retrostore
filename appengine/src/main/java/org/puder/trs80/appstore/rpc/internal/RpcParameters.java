/*
 * Copyright 2017, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.appstore.rpc.internal;

import com.google.common.base.Optional;

/**
 * Parameters for am RPC call.
 */
public interface RpcParameters {
  /** Returns an RPC parameter as an int, if it exists. */
  Optional<Integer> getInt(String name);

  /** Returns an RPC parameter as a string, if it exists. */
  Optional<String> getString(String name);
}
