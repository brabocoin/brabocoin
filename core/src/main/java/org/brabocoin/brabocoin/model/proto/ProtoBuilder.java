package org.brabocoin.brabocoin.model.proto;

/**
 * Indicates a builder for a {@link ProtoModel} class.
 */
public interface ProtoBuilder <P extends ProtoModel> {

    /**
     * Convert the builder object to a {@link ProtoModel} domain class instance.
     *
     * @return The domain class instance of the builder object.
     */
    <D extends P> D build();
}
