package com.betterreads.userbooks;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.betterreads.book.Book;
import com.betterreads.book.BookRepository;
import com.betterreads.user.BooksByUser;
import com.betterreads.user.BooksByUserRepository;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.datastax.oss.driver.internal.core.type.codec.TimeUuidCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;



@Controller
public class UserBooksController {

//    private final String COVER_IMAGE_ROOT = "http://covers.openlibrary.org/b/id/";

    @Autowired
    private UserBooksRepository userBooksRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BooksByUserRepository booksByUserRepository;

    @PostMapping("/addUserBook")
    public ModelAndView addBookForUser(
            @RequestBody MultiValueMap<String, String> formData,
            @AuthenticationPrincipal OAuth2User principal
    ) {

        String userId = principal.getAttribute("login");
        if(principal == null || userId == null) return null;

        String bookId = formData.getFirst("bookId");

        UserBooksPrimaryKey userBooksPrimaryKey = new UserBooksPrimaryKey();
        userBooksPrimaryKey.setUserId(userId);
        userBooksPrimaryKey.setBookId(bookId);

        UserBooks userBooks = new UserBooks();
        userBooks.setKey(userBooksPrimaryKey);
        userBooks.setStartedDate(LocalDate.parse(formData.getFirst("startDate")));
        userBooks.setCompletedDate(LocalDate.parse(formData.getFirst("completedDate")));
        userBooks.setRating(Integer.parseInt(formData.getFirst("rating")));
        userBooks.setReadingStatus(formData.getFirst("readingStatus"));

        userBooksRepository.save(userBooks);

        alsoSaveInBookByUser(userId, formData, bookId, userBooks);

        return new ModelAndView("redirect:/books/" + bookId);
    }

    private void alsoSaveInBookByUser(String userId, MultiValueMap<String, String> formData, String bookId, UserBooks userBooks) {

        BooksByUser booksByUser = new BooksByUser();

        Optional<Book> optBook = bookRepository.findById(bookId);
            Book book = optBook.get();

            booksByUser.setId(userId);
            booksByUser.setBookName(book.getName());
            booksByUser.setBookId(bookId);
            booksByUser.setCoverIds(book.getCoverIds());
            booksByUser.setAuthorNames(book.getAuthorNames());
            booksByUser.setRating(userBooks.getRating());
            booksByUser.setReadingStatus(userBooks.getReadingStatus());

            booksByUserRepository.save(booksByUser);

    }
}
