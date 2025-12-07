package telerik.project.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telerik.project.exceptions.EntityNotFoundException;
import telerik.project.helpers.AuthorizationHelper;
import telerik.project.helpers.validators.PostValidationHelper;
import telerik.project.models.Post;
import telerik.project.models.Tag;
import telerik.project.models.User;
import telerik.project.models.dtos.create.PostCreateDTO;
import telerik.project.models.dtos.update.PostUpdateDTO;
import telerik.project.models.filters.PostFilterOptions;
import telerik.project.repositories.PostRepository;
import telerik.project.repositories.UserRepository;
import telerik.project.repositories.specifications.PostSpecifications;
import telerik.project.services.contracts.NotificationService;
import telerik.project.services.contracts.PostService;
import telerik.project.services.contracts.TagService;
import telerik.project.utils.NormalizationUtils;
import telerik.project.utils.PaginationUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final TagService tagService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository,
                           TagService tagService,
                           NotificationService notificationService,
                           UserRepository userRepository) {
        this.postRepository = postRepository;
        this.tagService = tagService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAll(PostFilterOptions filterOptions) {
        Pageable pageable = PaginationUtils.createPageable(
                filterOptions.getPage(),
                filterOptions.getSize(),
                PostSpecifications.buildSort(filterOptions)
        );

        return postRepository
                .findAll(PostSpecifications.withFilters(filterOptions), pageable)
                .getContent().stream()
                .filter(p -> !p.isDeleted())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Post getById(Long id) {
        Post post = postRepository.findByIdWithLikes(id)
                .orElseThrow(() -> new EntityNotFoundException("Post", id));

        PostValidationHelper.validateNotDeleted(post);
        return post;
    }

    @Override
    @Transactional
    public Post create(PostCreateDTO dto, User authorParam) {
        User author = userRepository.findById(authorParam.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", authorParam.getId()));

        AuthorizationHelper.validateNotBlocked(author);

        Post post = new Post();
        post.setAuthor(author);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());

        if (dto.getTags() != null) {
            Set<Tag> resolved = dto.getTags().stream()
                    .map(NormalizationUtils::normalizeTagName)
                    .map(tagService::createIfNotExists)
                    .collect(Collectors.toSet());

            post.setTags(resolved);
        }

        postRepository.save(post);

        author.getFollowers().forEach(follower ->
                notificationService.send(
                        author,
                        follower,
                        post.getId(),
                        "POST",
                        "CREATE"
                )
        );

        return post;
    }

    @Override
    @Transactional
    public void update(Long postId, PostUpdateDTO dto, User actingUser) {
        AuthorizationHelper.validateNotBlocked(actingUser);

        Post existing = getById(postId);
        PostValidationHelper.validateNotDeleted(existing);
        AuthorizationHelper.validateOwner(actingUser, existing.getAuthor());

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            existing.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            existing.setContent(dto.getContent());
        }
        if (dto.getTags() != null) {
            Set<Tag> resolved = dto.getTags().stream()
                    .map(NormalizationUtils::normalizeTagName)
                    .map(tagService::createIfNotExists)
                    .collect(Collectors.toSet());

            existing.setTags(resolved);
        }

        postRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long postId, User actingUser) {
        AuthorizationHelper.validateNotBlocked(actingUser);

        Post post = getById(postId);
        PostValidationHelper.validateNotDeleted(post);
        AuthorizationHelper.validateOwnerOrAdmin(actingUser, post.getAuthor());

        post.setIsDeleted(true);
        postRepository.save(post);

        if (actingUser.isAdmin()) {
            notificationService.send(actingUser, post.getAuthor(), postId, "POST", "DELETED");
        }
    }

    @Override
    @Transactional
    public void likePost(Long postId, User actingUser) {
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post", postId));

        PostValidationHelper.validateNotDeleted(post);

        if (!post.getLikedByUsers().contains(actingUser)) {
            post.getLikedByUsers().add(actingUser);
            postRepository.save(post);
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, User actingUser) {
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post", postId));

        PostValidationHelper.validateNotDeleted(post);

        if (post.getLikedByUsers().contains(actingUser)) {
            post.getLikedByUsers().remove(actingUser);
            postRepository.save(post);
        }
    }

    @Override
    @Transactional
    public Post toggleLike(Long postId, User actingUser) {
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post", postId));

        PostValidationHelper.validateNotDeleted(post);

        if (post.getLikedByUsers().contains(actingUser)) {
            post.getLikedByUsers().remove(actingUser);
        } else {
            post.getLikedByUsers().add(actingUser);

            if (!actingUser.getId().equals(post.getAuthor().getId())) {
                try {
                    notificationService.send(actingUser, post.getAuthor(), postId, "POST", "LIKED");
                } catch (Exception e) {
                    System.err.println("Failed to send like notification: " + e.getMessage());
                }
            }
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAuthor(Long authorId) {
        return postRepository.countByAuthor_Id(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getByAuthorId(Long authorId) {
        return postRepository.findByAuthor_Id(authorId).stream()
                .filter(p -> !p.isDeleted())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getMostRecent() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(p -> !p.isDeleted())
                .limit(10)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getMostCommented() {
        return postRepository.findMostCommented().stream()
                .filter(p -> !p.isDeleted())
                .limit(10)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getByTags(List<String> tags) {
        return tags.stream()
                .flatMap(t -> postRepository.findByTags_Name(t).stream())
                .filter(p -> !p.isDeleted())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getLikedPosts(Long userId) {
        return postRepository.findByLikedByUsers_Id(userId).stream()
                .filter(p -> !p.isDeleted())
                .toList();
    }
}
