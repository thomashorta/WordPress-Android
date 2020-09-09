package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostDetail
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowVideoViewer
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockBlogUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.repository.usecases.UndoBlockBlogUseCase
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.ui.reader.usecases.ReaderPostBookmarkUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.PostFollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostCardActionsHandlerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var actionHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var bookmarkUseCase: ReaderPostBookmarkUseCase
    @Mock private lateinit var followUseCase: ReaderSiteFollowUseCase
    @Mock private lateinit var blockBlogUseCase: BlockBlogUseCase
    @Mock private lateinit var likeUseCase: PostLikeUseCase
    @Mock private lateinit var siteNotificationsUseCase: ReaderSiteNotificationsUseCase
    @Mock private lateinit var undoBlockBlogUseCase: UndoBlockBlogUseCase
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils

    @Before
    fun setUp() {
        actionHandler = ReaderPostCardActionsHandler(
                analyticsTrackerWrapper,
                reblogUseCase,
                bookmarkUseCase,
                followUseCase,
                blockBlogUseCase,
                likeUseCase,
                siteNotificationsUseCase,
                undoBlockBlogUseCase,
                appPrefsWrapper,
                dispatcher,
                resourceProvider,
                htmlMessageUtils,
                mock(),
                TEST_DISPATCHER,
                TEST_SCOPE,
                TEST_SCOPE
        )
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(false)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), anyOrNull())).thenReturn(mock())
    }

    /** BOOKMARK ACTION begin **/
    @Test
    fun `shows dialog when bookmark action is successful and shouldShowDialog returns true`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(true)

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowBookmarkedSavedOnlyLocallyDialog::class.java)
    }

    @Test
    fun `doesn't shows when dialog bookmark action is successful and shouldShowDialog returns false`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(false)

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValues.navigation).isEmpty()
    }

    @Test
    fun `shows snackbar on successful bookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)

        // Assert
        assertThat(observedValues.snackbarMsgs).isNotEmpty
    }

    @Test
    fun `Doesn't show snackbar on successful bookmark action when on bookmark(saved) tab`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        val isBookmarkList = true
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, isBookmarkList)

        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
    }

    @Test
    fun `Doesn't show snackbar on successful UNbookmark action`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(false)))

        val observedValues = startObserving()
        val isBookmarkList = true
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, isBookmarkList)

        // Assert
        assertThat(observedValues.snackbarMsgs).isEmpty()
    }

    @Test
    fun `navigates to bookmark tab on bookmark snackbar action clicked`() = test {
        // Arrange
        whenever(bookmarkUseCase.toggleBookmark(anyLong(), anyLong(), anyBoolean())).thenReturn(flowOf(Success(true)))

        val observedValues = startObserving()
        // Act
        actionHandler.onAction(dummyReaderPostModel(), BOOKMARK, false)
        observedValues.snackbarMsgs[0].buttonAction.invoke()

        // Assert
        assertThat(observedValues.navigation[0]).isEqualTo(ShowBookmarkedTab)
    }

    /** BOOKMARK ACTION end **/
    /** FOLLOW ACTION begin **/
    @Test
    fun `Emit followStatusUpdated after follow status update`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(flowOf(mock<PostFollowStatusChanged>()))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        assertThat(observedValues.followStatusUpdated).isNotEmpty
    }

    @Test
    fun `Fetch subscriptions after follow status update`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(flowOf(mock<PostFollowStatusChanged>()))

        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        verify(siteNotificationsUseCase).fetchSubscriptions()
    }

    @Test
    fun `Enable notifications snackbar shown when user follows a post`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(
                flowOf(PostFollowStatusChanged(-1, following = true, showEnableNotification = true))
        )
        val observedValues = startObserving()
        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        assertThat(observedValues.snackbarMsgs).isNotEmpty
    }

    @Test
    fun `Post notifications are disabled when user unfollows a post`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(
                flowOf(PostFollowStatusChanged(-1, following = false, deleteNotificationSubscription = true))
        )

        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        verify(siteNotificationsUseCase).updateSubscription(anyLong(), eq(SubscriptionAction.DELETE))
        verify(siteNotificationsUseCase).updateNotificationEnabledForBlogInDb(anyLong(), eq(false))
    }

    @Test
    fun `Post notifications are enabled when user clicks on enable notifications snackbar action`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(
                flowOf(PostFollowStatusChanged(-1, following = true, showEnableNotification = true))
        )
        val observedValues = startObserving()
        actionHandler.onAction(mock(), FOLLOW, false)

        // Act
        observedValues.snackbarMsgs[0].buttonAction.invoke()

        // Assert
        verify(siteNotificationsUseCase).updateSubscription(anyLong(), eq(SubscriptionAction.NEW))
        verify(siteNotificationsUseCase).updateNotificationEnabledForBlogInDb(anyLong(), eq(true))
    }

    @Test
    fun `Error message is shown when follow action fails with NoNetwork error`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(flowOf(NoNetwork))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        assertThat(observedValues.snackbarMsgs).isNotEmpty
    }

    @Test
    fun `Error message is shown when follow action fails with RequestFailed error`() = test {
        // Arrange
        whenever(followUseCase.toggleFollow(anyOrNull())).thenReturn(flowOf(RequestFailed))
        val observedValues = startObserving()

        // Act
        actionHandler.onAction(mock(), FOLLOW, false)

        // Assert
        assertThat(observedValues.snackbarMsgs).isNotEmpty
    }
    /** FOLLOW ACTION end **/

    @Test
    fun `Clicking on a post opens post detail`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleOnItemClicked(mock())

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowPostDetail::class.java)
    }

    @Test
    fun `Clicking on a video overlay opens video viewer`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleVideoOverlayClicked("mock")

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowVideoViewer::class.java)
    }

    @Test
    fun `Clicking on a header opens blog preview`() = test {
        // Arrange
        val observedValues = startObserving()

        // Act
        actionHandler.handleHeaderClicked(0L, 0L)

        // Assert
        assertThat(observedValues.navigation[0]).isInstanceOf(ShowBlogPreview::class.java)
    }

    private fun startObserving(): Observers {
        val navigation = mutableListOf<ReaderNavigationEvents>()
        actionHandler.navigationEvents.observeForever {
            navigation.add(it.peekContent())
        }

        val snackbarMsgs = mutableListOf<SnackbarMessageHolder>()
        actionHandler.snackbarEvents.observeForever {
            snackbarMsgs.add(it.peekContent())
        }

        val preloadPost = mutableListOf<PreLoadPostContent>()
        actionHandler.preloadPostEvents.observeForever {
            preloadPost.add(it.peekContent())
        }

        val followStatusUpdated = mutableListOf<PostFollowStatusChanged>()
        actionHandler.followStatusUpdated.observeForever {
            followStatusUpdated.add(it)
        }

        val refreshPosts = mutableListOf<Unit>()
        actionHandler.refreshPosts.observeForever {
            refreshPosts.add(it.peekContent())
        }
        return Observers(navigation, snackbarMsgs, preloadPost, followStatusUpdated, refreshPosts)
    }

    private fun dummyReaderPostModel(): ReaderPost {
        return ReaderPost().apply {
            postId = 1
            blogId = 1
        }
    }
}

private data class Observers(
    val navigation: List<ReaderNavigationEvents>,
    val snackbarMsgs: List<SnackbarMessageHolder>,
    val preloadPost: List<PreLoadPostContent>,
    val followStatusUpdated: List<PostFollowStatusChanged>,
    val refreshPosts: List<Unit>
)
