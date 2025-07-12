package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Poll;
import com.linknest.linknest.entity.PollOption;
import com.linknest.linknest.entity.PollVote;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.repository.PostRepository;
import com.linknest.linknest.repository.PollRepository;
import com.linknest.linknest.repository.PollOptionRepository;
import com.linknest.linknest.repository.PollVoteRepository;
import com.linknest.linknest.dto.PollCreateRequest;
import com.linknest.linknest.dto.PollVoteRequest;
import com.linknest.linknest.dto.PollResultResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/polls")
public class PollController {
    @Autowired
    private PollRepository pollRepository;
    @Autowired
    private PollOptionRepository pollOptionRepository;
    @Autowired
    private PollVoteRepository pollVoteRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<?> createPoll(@PathVariable Long postId, @RequestBody PollCreateRequest request) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (pollRepository.findById(postId).isPresent()) {
            return ResponseEntity.badRequest().body("Poll already exists for this post");
        }
        Poll poll = new Poll(post, request.getQuestion());
        List<PollOption> options = request.getOptions().stream().map(opt -> new PollOption(poll, opt)).collect(Collectors.toList());
        poll.setOptions(options);
        pollRepository.save(poll);
        pollOptionRepository.saveAll(options);
        return ResponseEntity.ok("Poll created");
    }

    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody PollVoteRequest request) {
        User user = getCurrentUser();
        PollOption option = pollOptionRepository.findById(request.getOptionId()).orElseThrow(() -> new RuntimeException("Option not found"));
        Poll poll = option.getPoll();
        if (pollVoteRepository.existsByUserAndPollOption(user, option)) {
            return ResponseEntity.badRequest().body("User has already voted in this poll");
        }
        option.setVoteCount(option.getVoteCount() + 1);
        pollOptionRepository.save(option);
        pollVoteRepository.save(new PollVote(option, user));
        return ResponseEntity.ok("Vote recorded");
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<PollResultResponse> getPoll(@PathVariable Long postId) {
        User user = getCurrentUser();
        Poll poll = pollRepository.findById(postId).orElseThrow(() -> new RuntimeException("Poll not found"));
        List<PollOption> options = pollOptionRepository.findByPoll(poll);
        Optional<PollVote> userVote = options.stream()
            .map(opt -> pollVoteRepository.findByUserAndPollOption(user, opt))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        Long userVotedOptionId = userVote.map(v -> v.getPollOption().getId()).orElse(null);
        List<PollResultResponse.OptionResult> optionResults = options.stream()
            .map(opt -> new PollResultResponse.OptionResult(opt.getId(), opt.getText(), opt.getVoteCount()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(new PollResultResponse(poll.getQuestion(), optionResults, userVotedOptionId));
    }
} 