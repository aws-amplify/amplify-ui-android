package com.amplifyframework.ui.liveness

import android.Manifest
import android.content.Context
import android.graphics.RectF
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin
import com.amplifyframework.predictions.aws.models.ColorChallenge
import com.amplifyframework.predictions.aws.models.ColorChallengeType
import com.amplifyframework.predictions.aws.models.ColorDisplayInformation
import com.amplifyframework.predictions.aws.models.FaceTargetChallenge
import com.amplifyframework.predictions.aws.models.FaceTargetMatchingParameters
import com.amplifyframework.predictions.aws.models.RgbColor
import com.amplifyframework.predictions.models.FaceLivenessSession
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import com.amplifyframework.predictions.options.FaceLivenessSessionOptions
import com.amplifyframework.ui.liveness.camera.FrameAnalyzer
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.model.LivenessCheckState
import com.amplifyframework.ui.liveness.state.LivenessState
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import io.mockk.CapturingSlot
import io.mockk.InvokeMatcher
import io.mockk.OfTypeMatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

// mock calls to Rekognition service, just make sure the flow functions as normal
// steps:
// 1. start the flow
// 2. click the button to start the liveness session
// 3. verify that the flow was started and shows correct face distance UI
// 4. trigger fake response that the face is at the right distance
// 5. verify that the flow displays colored rectangles
// 6. verify that the component is sending the video feed through the fake websocket
// 7. send fake correct/incorrect response
class LivenessFlowInstrumentationTest {
    private lateinit var livenessSessionInformation: CapturingSlot<FaceLivenessSessionInformation>
    private lateinit var livenessSessionOptions: CapturingSlot<FaceLivenessSessionOptions>
    private lateinit var onSessionStarted: CapturingSlot<Consumer<FaceLivenessSession>>
    private lateinit var onLivenessComplete: CapturingSlot<Action>
    private lateinit var tooCloseString: String
    private lateinit var beginCheckString: String
    private lateinit var noFaceString: String
    private lateinit var multipleFaceString: String
    private lateinit var connectingString: String
    private lateinit var moveCloserString: String
    private lateinit var holdStillString: String

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    var mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

        livenessSessionInformation = slot()
        livenessSessionOptions = slot()
        onSessionStarted = slot()
        onLivenessComplete = slot()
        mockkStatic(AWSPredictionsPlugin::class)
        every {
            AWSPredictionsPlugin.startFaceLivenessSession(
                any(), // sessionId
                capture(livenessSessionInformation), // sessionInformation
                capture(livenessSessionOptions), // options
                capture(onSessionStarted), // onSessionStarted
                capture(onLivenessComplete), // onComplete
                any(), // onError
            )
        } just Runs

        // string resources
        beginCheckString = context.getString(R.string.amplify_ui_liveness_get_ready_begin_check)
        tooCloseString = context.getString(R.string.amplify_ui_liveness_challenge_instruction_move_face_further)
        noFaceString = context.getString(R.string.amplify_ui_liveness_challenge_instruction_move_face)
        multipleFaceString = context.getString(
            R.string.amplify_ui_liveness_challenge_instruction_multiple_faces_detected,
        )
        connectingString = context.getString(R.string.amplify_ui_liveness_challenge_connecting)
        moveCloserString = context.getString(R.string.amplify_ui_liveness_challenge_instruction_move_face_closer)
        holdStillString = context.getString(
            R.string.amplify_ui_liveness_challenge_instruction_hold_face_during_freshness,
        )
    }

    @Test
    fun testLivenessDefaultCameraGivesNoFaceError() {
        val sessionId = "sessionId"
        composeTestRule.setContent {
            FaceLivenessDetector(sessionId = sessionId, region = "us-east-1", onComplete = {
            }, onError = { assertTrue(false) })
        }

        composeTestRule.onNodeWithText(beginCheckString).assertExists()
        composeTestRule.onNodeWithText(beginCheckString).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(noFaceString)
                .fetchSemanticsNodes().size == 1
        }
        // make sure compose flow reaches this point
        composeTestRule.onNodeWithText(noFaceString).assertIsDisplayed()
    }

    @Test
    fun testLivenessFlowTooClose() {
        mockkConstructor(FrameAnalyzer::class)
        var livenessState: LivenessState? = null
        every {
            constructedWith<FrameAnalyzer>(
                OfTypeMatcher<Context>(Context::class),
                InvokeMatcher<LivenessState> {
                    livenessState = it
                },
            ).analyze(any())
        } answers {
            assert(livenessState != null)

            livenessState?.onFrameFaceCountUpdate(1)

            // Features too far apart, this face must be too close to the camera
            livenessState?.onFrameFaceUpdate(
                RectF(0f, 0f, 400f, 400f),
                FaceDetector.Landmark(120f, 120f),
                FaceDetector.Landmark(280f, 120f),
                FaceDetector.Landmark(200f, 320f),
            )
        }

        val sessionId = "sessionId"
        composeTestRule.setContent {
            FaceLivenessDetector(sessionId = sessionId, region = "us-east-1", onComplete = {
            }, onError = { assertTrue(false) })
        }

        composeTestRule.onNodeWithText(beginCheckString).assertExists()
        composeTestRule.onNodeWithText(beginCheckString).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(tooCloseString)
                .fetchSemanticsNodes().size == 1
        }

        // make sure compose flow reaches this point
        composeTestRule.onNodeWithText(tooCloseString).assertIsDisplayed()

        unmockkConstructor(FrameAnalyzer::class)
    }

    @Test
    fun testLivenessFlowTooManyFaces() {
        mockkConstructor(FrameAnalyzer::class)
        var livenessState: LivenessState? = null
        every {
            constructedWith<FrameAnalyzer>(
                OfTypeMatcher<Context>(Context::class),
                InvokeMatcher<LivenessState> {
                    livenessState = it
                },
            ).analyze(any())
        } answers {
            assert(livenessState != null)

            livenessState?.onFrameFaceCountUpdate(2)
        }

        val sessionId = "sessionId"
        composeTestRule.setContent {
            FaceLivenessDetector(sessionId = sessionId, region = "us-east-1", onComplete = {
            }, onError = { assertTrue(false) })
        }

        composeTestRule.onNodeWithText(beginCheckString).assertExists()
        composeTestRule.onNodeWithText(beginCheckString).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(multipleFaceString)
                .fetchSemanticsNodes().size == 1
        }

        // make sure compose flow reaches this point
        composeTestRule.onNodeWithText(multipleFaceString).assertIsDisplayed()

        unmockkConstructor(FrameAnalyzer::class)
    }

    @Test
    fun testLivenessFlowNoChallenges() {
        mockkConstructor(FrameAnalyzer::class)
        var livenessState: LivenessState? = null
        every {
            constructedWith<FrameAnalyzer>(
                OfTypeMatcher<Context>(Context::class),
                InvokeMatcher<LivenessState> {
                    livenessState = it
                },
            ).analyze(any())
        } answers {
            assert(livenessState != null)

            livenessState?.onFrameFaceCountUpdate(1)

            // Features should be sized correctly here
            livenessState?.onFrameFaceUpdate(
                RectF(0f, 0f, 200f, 200f),
                FaceDetector.Landmark(60f, 60f),
                FaceDetector.Landmark(140f, 60f),
                FaceDetector.Landmark(100f, 160f),
            )
        }

        val sessionId = "sessionId"
        var completesSuccessfully = false
        composeTestRule.setContent {
            FaceLivenessDetector(sessionId = sessionId, region = "us-east-1", onComplete = {
                completesSuccessfully = true
            }, onError = { assertTrue(false) })
        }

        composeTestRule.onNodeWithText(beginCheckString).assertExists()
        composeTestRule.onNodeWithText(beginCheckString).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(connectingString)
                .fetchSemanticsNodes().size == 1
        }

        onSessionStarted.captured.accept(FaceLivenessSession(emptyList(), {}, {}, {}))

        composeTestRule.waitForIdle()

        onLivenessComplete.captured.call()
        assertTrue(completesSuccessfully)

        unmockkConstructor(FrameAnalyzer::class)
    }

    @Test
    fun testLivenessFlowWithChallenges() {
        mockkConstructor(FrameAnalyzer::class)
        var livenessState: LivenessState? = null
        every {
            constructedWith<FrameAnalyzer>(
                OfTypeMatcher<Context>(Context::class),
                InvokeMatcher<LivenessState> {
                    livenessState = it
                },
            ).analyze(any())
        } answers {
            assert(livenessState != null)

            livenessState?.onFrameFaceCountUpdate(1)

            // Features should be sized correctly here
            livenessState?.onFrameFaceUpdate(
                RectF(0f, 0f, 200f, 200f),
                FaceDetector.Landmark(60f, 60f),
                FaceDetector.Landmark(140f, 60f),
                FaceDetector.Landmark(100f, 160f),
            )
        }

        val sessionId = "sessionId"
        var completesSuccessfully = false
        composeTestRule.setContent {
            FaceLivenessDetector(sessionId = sessionId, region = "us-east-1", onComplete = {
                completesSuccessfully = true
            }, onError = { assertTrue(false) })
        }

        composeTestRule.onNodeWithText(beginCheckString).assertExists()
        composeTestRule.onNodeWithText(beginCheckString).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(connectingString)
                .fetchSemanticsNodes().size == 1
        }

        val faceTargetMatchingParameters = mockk<FaceTargetMatchingParameters>()
        every { faceTargetMatchingParameters.targetIouThreshold }.returns(0.7f)
        every { faceTargetMatchingParameters.targetIouWidthThreshold }.returns(0.25f)
        every { faceTargetMatchingParameters.targetIouHeightThreshold }.returns(0.25f)
        every { faceTargetMatchingParameters.faceIouWidthThreshold }.returns(0.15f)
        every { faceTargetMatchingParameters.faceIouHeightThreshold }.returns(0.15f)

        val faceTargetChallenge = mockk<FaceTargetChallenge>()
        val faceRect = RectF(19f, -49f, 441f, 633f)
        every { faceTargetChallenge.targetWidth }.returns(faceRect.right - faceRect.left)
        every { faceTargetChallenge.targetHeight }.returns(faceRect.bottom - faceRect.top)
        every { faceTargetChallenge.targetCenterX }.returns((faceRect.left + faceRect.right) / 2)
        every { faceTargetChallenge.targetCenterY }.returns((faceRect.top + faceRect.bottom) / 2)
        every { faceTargetChallenge.faceTargetMatching }.returns(faceTargetMatchingParameters)

        val colors = listOf(
            RgbColor(0, 0, 0),
            RgbColor(0, 255, 255),
            RgbColor(255, 0, 0),
            RgbColor(0, 255, 0),
            RgbColor(0, 0, 255),
            RgbColor(255, 255, 0),
            RgbColor(0, 255, 0),
            RgbColor(255, 0, 0),
        )
        val durations = listOf(
            75f,
            475f,
            475f,
            475f,
            475f,
            475f,
            475f,
            475f,
        )
        val challengeColors = List(colors.size) {
            val colorDisplayInformation = mockk<ColorDisplayInformation>()
            every { colorDisplayInformation.color }.returns(colors[it])
            every { colorDisplayInformation.duration }.returns(durations[it])
            every { colorDisplayInformation.shouldScroll }.returns(false)
            colorDisplayInformation
        }
        val colorChallenge = mockk<ColorChallenge>()
        every { colorChallenge.challengeId }.returns("id")
        every { colorChallenge.challengeType }.returns(ColorChallengeType.SEQUENTIAL)
        every { colorChallenge.challengeColors }.returns(challengeColors)

        onSessionStarted.captured.accept(
            FaceLivenessSession(
                listOf(faceTargetChallenge, colorChallenge),
                {}, // onVideoEvent
                {}, // onChallengeResponseEvent
                {}, // stopLivenessSession
            ),
        )

        // update face location to show oval
        livenessState?.onFrameFaceUpdate(
            RectF(0f, 0f, 400f, 400f),
            FaceDetector.Landmark(60f, 60f),
            FaceDetector.Landmark(140f, 60f),
            FaceDetector.Landmark(100f, 160f),
        )

        // in the same spot as it was originally, the face is too far
        composeTestRule.waitUntil(1000) {
            composeTestRule.onAllNodesWithText(moveCloserString)
                .fetchSemanticsNodes().size == 1
        }

        composeTestRule.waitForIdle()

        // update face to be inside the oval position
        livenessState?.onFrameFaceUpdate(
            faceRect,
            FaceDetector.Landmark(60f, 60f),
            FaceDetector.Landmark(140f, 60f),
            FaceDetector.Landmark(100f, 160f),
        )

        // now, the face is inside the oval.  wait for the colors to finish
        composeTestRule.waitForIdle()

        assertTrue(livenessState?.readyToSendFinalEvents == true)
        val state = livenessState?.livenessCheckState?.value
        assertTrue(state is LivenessCheckState.Success)
        assertTrue((state as LivenessCheckState.Success).faceGuideRect == faceRect)

        onLivenessComplete.captured.call()
        assertTrue(completesSuccessfully)

        unmockkConstructor(FrameAnalyzer::class)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupAmplify() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

            // mock the Amplify Auth category
            val authPlugin = AWSCognitoAuthPlugin()
            mockkObject(authPlugin)
            every { authPlugin.fetchAuthSession(any(), any()) } answers {
                firstArg<(AuthSession) -> Unit>().invoke(AuthSession(true))
            }
            Amplify.addPlugin(authPlugin)
            Amplify.configure(context)
        }
    }
}
