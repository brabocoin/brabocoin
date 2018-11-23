package org.brabocoin.brabocoin.model.proto;

/**
 * Indicates that the implementing class is a data model that is converted to and from Protobuf
 * objects.
 */
public interface ProtoModel<M extends ProtoModel> {

    /**
     * Retrieves the builder for this class.
     *
     * @return The builder for this class.
     */
    Class<? extends ProtoBuilder> getBuilder();

}
