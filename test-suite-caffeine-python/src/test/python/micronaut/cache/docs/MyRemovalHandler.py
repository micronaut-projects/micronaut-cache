from com.github.benmanes.caffeine.cache import RemovalCause
from jakarta.inject import Singleton


@Singleton
class MyRemovalHandler:

    def __init__(self):
        self.removals: list[str] = []

    def handle(self, key: str, value: int, cause: RemovalCause) -> None:
        self.removals.append(f"{key}|{value}|{cause}")

    def get_removals(self) -> list[str]:
        return self.removals
