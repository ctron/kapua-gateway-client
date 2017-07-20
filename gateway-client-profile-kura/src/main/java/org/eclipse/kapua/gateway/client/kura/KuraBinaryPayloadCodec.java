/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.gateway.client.kura;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.kura.internal.Metrics;
import org.eclipse.kapua.gateway.client.kura.payload.KuraPayloadProto;
import org.eclipse.kapua.gateway.client.kura.payload.KuraPayloadProto.KuraPayload;
import org.eclipse.kapua.gateway.client.utils.Buffers;

public class KuraBinaryPayloadCodec implements BinaryPayloadCodec {

    public static class Builder {

        public KuraBinaryPayloadCodec build() {
            return new KuraBinaryPayloadCodec();
        }
    }

    private KuraBinaryPayloadCodec() {
    }

    @Override
    public ByteBuffer encode(final Payload payload, final ByteBuffer buffer) throws Exception {

        Objects.requireNonNull(payload);

        final KuraPayloadProto.KuraPayload.Builder builder = KuraPayload.newBuilder();
        builder.setTimestamp(payload.getTimestamp().getTime());
        Metrics.buildMetrics(builder, payload.getValues());

        final byte[] data = builder.build().toByteArray();

        if (buffer == null) {
            // create a wrapped buffer
            return Buffers.wrap(data);
        } else if (buffer.remaining() < data.length) {
            // create a new, merged buffer
            buffer.flip();
            final ByteBuffer newBuffer = ByteBuffer.allocate(buffer.remaining() + data.length);
            newBuffer.put(buffer);
            newBuffer.put(data);
            return newBuffer;
        } else {
            buffer.put(data);
            return buffer;
        }
    }

    @Override
    public Payload decode(final ByteBuffer buffer) throws Exception {
        Objects.requireNonNull(buffer);

        final KuraPayload payload = KuraPayload.parseFrom(Buffers.toByteArray(buffer));
        final Map<String, Object> values = Metrics.extractMetrics(payload);
        return Payload.of(payload.getTimestamp()+"", values);
    }

}
