from jakarta.inject import Singleton
from micronaut.cache.hazelcast import HazelcastClientConfiguration
from micronaut.context.event import BeanCreatedEvent, BeanCreatedEventListener


# tag::clazz[]
@Singleton
class HazelcastAdditionalSettings(BeanCreatedEventListener[HazelcastClientConfiguration]):

    def onCreated(self, event: BeanCreatedEvent[HazelcastClientConfiguration]) -> HazelcastClientConfiguration:
        configuration = event.getBean()
        # Set anything on the configuration
        configuration.setClusterName("dev")

        return configuration
# end::clazz[]
