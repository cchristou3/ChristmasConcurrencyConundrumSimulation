package CO3401.AdvancedProducerConsumer;

/**
 * Describes the type of the {@link Connection} instance.
 * <p> - {@link ConnectionType#InputBelt} refers to a Conveyor
 * that has presents from it taken away.
 * <p> - {@link ConnectionType#OutputBelt} refers to a Conveyor
 * that has presents to it placed.
 * <p> - {@link ConnectionType#InputBelt} refers to a Sack
 * that has presents to it placed.
 *
 * @author anonymous
 */
public enum ConnectionType {
    InputBelt,
    OutputBelt,
    OutputSack
}
