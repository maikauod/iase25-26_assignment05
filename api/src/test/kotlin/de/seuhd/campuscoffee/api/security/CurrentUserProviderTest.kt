package de.seuhd.campuscoffee.api.security

import de.seuhd.campuscoffee.domain.ports.api.UserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

/**
 * Tests [CurrentUserProvider], the bridge from the Spring Security principal to the domain [User].
 */
class CurrentUserProviderTest {

    private val userService: UserService = mock()
    private val currentUserProvider = CurrentUserProvider(userService)

    @BeforeEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `returns domain user for authenticated principal`() {
        // given
        val auth = UsernamePasswordAuthenticationToken("alice", "password")
        SecurityContextHolder.getContext().authentication = auth

        val expectedUser = User(id = 1, loginName = "alice")

        whenever(userService.getByLoginName("alice")).thenReturn(expectedUser)

        // when
        val result = currentUserProvider.currentUser()

        // then
        assertEquals(expectedUser, result)
    }

    @Test
    fun `throws when no authentication present`() {
        // given
        SecurityContextHolder.clearContext()

        // when / then
        assertThrows(IllegalStateException::class.java) {
            currentUserProvider.currentUser()
        }
    }
}
