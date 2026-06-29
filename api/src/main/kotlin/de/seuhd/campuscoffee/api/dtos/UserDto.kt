package de.seuhd.campuscoffee.api.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import de.seuhd.campuscoffee.domain.model.objects.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * DTO for user metadata. Properties are nullable, so a request body that omits a field deserializes and
 * is then rejected by bean validation; the controller validates the DTO before it is mapped to a
 * [de.seuhd.campuscoffee.domain.model.objects.User].
 *
 * [password] is write-only: a client may send it on create/update but it is never serialized in a
 * response (and the stored hash is never exposed at all). It is optional in the starter; the assignment
 * (Exercise 1) makes it required with a minimum length. [roles] is returned in responses.
 */
data class UserDto(
    override val id: Long? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @field:NotNull
    @field:Size(min = 1, max = 255, message = "Login name must be between 1 and 255 characters long.")
    @field:Pattern(regexp = "\\w+", message = "Login name can only contain word characters: [a-zA-Z_0-9]+")
    val loginName: String?,
    @field:NotNull
    @field:Email
    // @Email alone admits addresses longer than the 254-character column, which would surface as a 500
    @field:Size(max = 254, message = "Email address must be at most 254 characters long.")
    val emailAddress: String?,
    @field:NotNull
    @field:Size(min = 1, max = 255, message = "First name must be between 1 and 255 characters long.")
    val firstName: String?,
    @field:NotNull
    @field:Size(min = 1, max = 255, message = "Last name must be between 1 and 255 characters long.")
    val lastName: String?,
    //  (Exercise 1): make the password required and at least 8 characters (firstName and lastName
    //  above show the bean-validation pattern to follow).
    @field:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @field:NotNull
    @field:Size(min = 8, max = 255, message = "password must be at least 8 characters long")
    val password: String?,
    val roles: Set<Role>? = null
) : Dto<Long>
