package org.brabocoin.brabocoin.util;

import org.awaitility.core.ConditionTimeoutException;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;

/**
 * The {@link Destructible} class allows for the checked destruction of objects.
 * A supplier of an object is given, assuming no hard of soft reference exists to the supplied object.
 * <p>
 * The only hard reference to the object is the private field {@link #object}.
 * A phantom reference with a reference queue is created to check for object destruction.
 * <p>
 * Getting a reference to the object is done through {@link #getReference()}.
 * This returns a weak reference that is null when the object is destroyed through {@link #destruct()}.
 *
 * @param <U> Class of the object to be made destructible.
 */
public class Destructible<U> {
    /**
     * The single hard reference to the object, preventing garbage collection.
     */
    @Nullable
    private U object;

    /**
     * The reference queue for the phantom reference
     */
    private ReferenceQueue<U> referenceQueue;

    /**
     * The phantom reference to the {@link #object}.
     */
    private PhantomReference<U> reference;

    /**
     * Whether the object is destroyed.
     */
    private boolean destroyed;

    /**
     * Create a destructible object for the object supplier.
     * Assumes the supplied object is not instantiated or no reference exists to the given object.
     *
     * @param objectSupplier The supplier of the destructible object.
     */
    public Destructible(Supplier<U> objectSupplier) {
        this.object = objectSupplier.get();

        referenceQueue = new ReferenceQueue<>();
        reference = new PhantomReference<>(object, referenceQueue);
        destroyed = false;
    }

    /**
     * Gets a weak reference to the object.
     * Note that this reference returns {@code null} when the object is destructed.
     *
     * @return Weak reference to the object
     */
    public WeakReference<U> getReference() {
        return new WeakReference<>(object);
    }

    /**
     * Destruct the object, waiting for it to be processed by the garbage collector.
     * <p>
     * Dereferences the object and waits for the phantom reference to be enqueued
     * (indicating phantom reachability of the object).
     * The phantom reference itself is cleared and the queue is truncated.
     *
     * @throws DestructionException When enqueuement times out or the queue was not cleared before timeout.
     */
    public void destruct() throws DestructionException {
        // Dereference object
        object = null;

        try {
            await().atMost(Constants.JIT_PHANTOM_ENQUEUE_TIMEOUT, TimeUnit.SECONDS)
                    .pollDelay(Constants.JIT_POLL_DELAY, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .until(() -> {
                        // Hint garbage collector
                        System.gc();

                        return reference.isEnqueued();
                    });
        } catch (ConditionTimeoutException e) {
            throw new DestructionException("Object was not enqueued in phantom reference queue.");
        }

        // Prematurely clear reference
        reference.clear();

        try {
            await().atMost(Constants.JIT_OBJECT_DESTRUCTION_TIMEOUT, TimeUnit.SECONDS)
                    .pollDelay(Constants.JIT_POLL_DELAY, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .until(() -> {
                        Reference ref = referenceQueue.remove();
                        if (ref != null) {
                            ref.clear();
                        }

                        return referenceQueue.poll() == null;
                    });
        } catch (ConditionTimeoutException e) {
            throw new DestructionException("Queue was not cleared before destruction timeout.");
        }

        destroyed = true;
    }

    /**
     * Gets whether the objects is destroyed.
     *
     * @return Whether the object is destroyed.
     */
    public boolean isDestroyed() {
        return destroyed;
    }
}
