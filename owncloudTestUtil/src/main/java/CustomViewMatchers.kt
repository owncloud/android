import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Description

fun withChildViewCount(count: Int, childMatcher: Matcher<View>): Matcher<View> {
    return object : BoundedMatcher<View, ViewGroup>(ViewGroup::class.java) {
        override fun matchesSafely(viewGroup: ViewGroup): Boolean {
            var matchCount = 0
            for (i in 0 until viewGroup.childCount) {
                if (childMatcher.matches(viewGroup.getChildAt(i))) {
                    matchCount++
                }
            }
            return matchCount == count
        }

        override fun describeTo(description: Description?) {
            description?.appendText("ViewGroup with child-count=$count and")
            childMatcher.describeTo(description)
        }
    }
}

fun nthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            if (view.parent !is ViewGroup) {
                return parentMatcher.matches(view.parent)
            }
            val group = view.parent as ViewGroup
            return parentMatcher.matches(view.parent) && group.getChildAt(childPosition) == view
        }

        override fun describeTo(description: Description) {
            description.appendText("with $childPosition child view of type parentMatcher")
        }
    }
}
