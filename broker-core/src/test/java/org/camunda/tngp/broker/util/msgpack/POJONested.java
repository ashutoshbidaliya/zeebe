/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.broker.util.msgpack;

import org.camunda.tngp.broker.util.msgpack.property.LongProperty;

public class POJONested extends UnpackedObject
{
    private final LongProperty longProp = new LongProperty("foo", -1L);

    public POJONested()
    {
        this.declareProperty(longProp);
    }

    public POJONested setLong(long value)
    {
        this.longProp.setValue(value);
        return this;
    }

    public long getLong()
    {
        return this.longProp.getValue();
    }
}
