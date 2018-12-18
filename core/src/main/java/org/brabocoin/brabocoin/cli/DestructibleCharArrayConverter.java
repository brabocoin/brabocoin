package org.brabocoin.brabocoin.cli;

import com.beust.jcommander.IStringConverter;
import org.brabocoin.brabocoin.util.Destructible;

/**
 * Convert to a destructible character array.
 */
public class DestructibleCharArrayConverter implements IStringConverter<Destructible<char[]>> {

    @Override
    public Destructible<char[]> convert(String value) {
        return new Destructible<>(value::toCharArray);
    }
}
