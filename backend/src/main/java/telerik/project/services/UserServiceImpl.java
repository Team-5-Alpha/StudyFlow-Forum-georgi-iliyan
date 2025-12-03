package telerik.project.services;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telerik.project.exceptions.EntityDuplicateException;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.helpers.AuthorizationHelper;
import telerik.project.helpers.validators.ActionValidationHelper;
import telerik.project.helpers.validators.UserValidationHelper;
import telerik.project.models.Post;
import telerik.project.models.Role;
import telerik.project.models.User;
import telerik.project.models.filters.UserFilterOptions;
import telerik.project.repositories.UserRepository;
import telerik.project.repositories.specifications.UserSpecifications;
import telerik.project.services.contracts.NotificationService;
import telerik.project.services.contracts.PostService;
import telerik.project.services.contracts.UserService;
import telerik.project.utils.NormalizationUtils;
import telerik.project.utils.PaginationUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostService postService;
    private final NotificationService notificationService;

    public UserServiceImpl(
            UserRepository userRepository,
            PostService postService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.postService = postService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAll(UserFilterOptions filterOptions) {
        Pageable pageable = PaginationUtils.createPageable(
                filterOptions.getPage(),
                filterOptions.getSize(),
                UserSpecifications.buildSort(filterOptions)
        );

        return userRepository
                .findAll(UserSpecifications.withFilters(filterOptions), pageable)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", "email", email));
    }

    @Override
    @Transactional
    public void create(User user) {
        String normalizedEmail = NormalizationUtils.normalizationEmail(user.getEmail());
        String normalizedUsername = NormalizationUtils.normalizationUsername(user.getUsername());

        UserValidationHelper.validateUsernameNotTaken(userRepository, normalizedUsername);
        UserValidationHelper.validateEmailNotTaken(userRepository, normalizedEmail);

        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);

        // Todo: security phase - encode password
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void update(Long userId, User updatedUser, User actingUser) {
        AuthorizationHelper.validateNotBlocked(actingUser);

        User existing = getById(userId);
        AuthorizationHelper.validateOwner(actingUser, existing);

        String normalizedEmail = NormalizationUtils.normalizationEmail(updatedUser.getEmail());
        UserValidationHelper.validateEmailAvailable(userRepository, normalizedEmail, existing.getEmail());

        existing.setFirstName(updatedUser.getFirstName());
        existing.setLastName(updatedUser.getLastName());
        existing.setEmail(normalizedEmail);

        if (existing.isAdmin()) {
            existing.setPhoneNumber(updatedUser.getPhoneNumber());
        }

        existing.setPassword(updatedUser.getPassword()); //Todo: Security change later
        existing.setProfilePhotoURL(updatedUser.getProfilePhotoURL());

        userRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long targetUserId, User actingUser) {
        User user = getById(targetUserId);
        AuthorizationHelper.validateOwnerOrAdmin(actingUser, user);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void blockUser(Long targetUserId, User actingUser) {
        AuthorizationHelper.validateAdmin(actingUser);
        AuthorizationHelper.validateSelfOperationNotAllowed(actingUser, targetUserId);

        User target = getById(targetUserId);
        ActionValidationHelper.validateCanBlock(target);

        target.setIsBlocked(true);
        userRepository.save(target);

        notificationService.send(
                actingUser,
                target,
                targetUserId,
                "USER",
                "BLOCK"
        );
    }

    @Override
    @Transactional
    public void unblockUser(Long targetUserId, User actingUser) {
        AuthorizationHelper.validateAdmin(actingUser);
        AuthorizationHelper.validateSelfOperationNotAllowed(actingUser, targetUserId);

        User target = getById(targetUserId);
        ActionValidationHelper.validateCanUnblock(target);

        target.setIsBlocked(false);
        userRepository.save(target);

        notificationService.send(
                actingUser,
                target,
                targetUserId,
                "USER",
                "UNBLOCK"
        );
    }

    @Override
    @Transactional
    public void promoteToAdmin(Long targetUserId, User actingUser) {
        AuthorizationHelper.validateAdmin(actingUser);
        AuthorizationHelper.validateSelfOperationNotAllowed(actingUser, targetUserId);

        User target = getById(targetUserId);
        ActionValidationHelper.validateCanPromote(target);

        target.setRole(Role.ADMIN);
        userRepository.save(target);

        notificationService.send(
                actingUser,
                target,
                targetUserId,
                "ADMIN",
                "PROMOTE"
        );
    }

    @Override
    @Transactional
    public List<Post> getPostsByUser(Long userId) {
        return postService.getByAuthorId(userId);
    }

    @Override
    @Transactional
    public void followUser(Long id, User actingUserParam) {
        // 1. ПРЕЗАРЕЖДАНЕ НА ACTING USER (За да избегнем LazyInitializationException)
        User actingUser = userRepository.findById(actingUserParam.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", actingUserParam.getId()));

        // 2. Намираме човека, когото искаме да последваме
        User userToFollow = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        // Валидации
        AuthorizationHelper.validateNotBlocked(actingUser);
        AuthorizationHelper.validateNotBlocked(userToFollow);
        AuthorizationHelper.validateSelfOperationNotAllowed(actingUser, id);

        // Логика
        if (actingUser.getFollowing().contains(userToFollow)) {
            // Тук използваме твоя къстъм ексепшън
            throw new EntityDuplicateException("User", "follow relationship", "already exists");
        }

        actingUser.getFollowing().add(userToFollow);
        userRepository.save(actingUser);

        // Нотификация
        notificationService.send(actingUser, userToFollow, actingUser.getId(), "USER", "FOLLOW");
    }

    @Override
    @Transactional
    public void unfollowUser(Long id, User actingUserParam) {
        // 1. ПРЕЗАРЕЖДАНЕ НА ACTING USER
        User actingUser = userRepository.findById(actingUserParam.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", actingUserParam.getId()));

        // 2. Намираме човека за отследване
        User userToUnfollow = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        // Валидации
        AuthorizationHelper.validateNotBlocked(actingUser);
        AuthorizationHelper.validateNotBlocked(userToUnfollow);

        // Логика
        if (!actingUser.getFollowing().contains(userToUnfollow)) {
            // Хвърляме грешка, ако връзката не съществува
            throw new EntityNotFoundException("Relationship", "follower", "not found");
        }

        actingUser.getFollowing().remove(userToUnfollow);
        userRepository.save(actingUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getFollowers(Long userId) {
        return List.copyOf(getById(userId).getFollowers());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getFollowing(Long userId) {
        return List.copyOf(getById(userId).getFollowing());
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return getById(userId).getFollowers().size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return getById(userId).getFollowing().size();
    }
}
