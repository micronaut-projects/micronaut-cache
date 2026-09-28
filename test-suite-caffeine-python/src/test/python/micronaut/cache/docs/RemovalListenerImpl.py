from com.github.benmanes.caffeine.cache import RemovalCause, RemovalListener
from jakarta.inject import Singleton

from .MyRemovalHandler import MyRemovalHandler


# tag::clazz[]
@Singleton
class RemovalListenerImpl(RemovalListener[str, int]):

    def __init__(self, handler: MyRemovalHandler):
        self.handler = handler

    def onRemoval(self, key: str | None, value: int | None, cause: RemovalCause) -> None:
        self.handler.handle(key, value, cause)
# end::clazz[]
