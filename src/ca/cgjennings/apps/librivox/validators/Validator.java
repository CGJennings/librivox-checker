package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.*;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;

/**
 * An object that is capable of analyzing a {@link LibriVoxAudioFile}.
 * Validators are discovered at runtime by the {@link ValidatorFactory},
 * which reads a list of validators from <tt>resources/validators.classlist</tt>.
 * Validators must be listed in this file and implement a public no-arg
 * constructor in order to be discovered and used by the validation tool.
 * <p>
 * A validator can report the results of an analysis as information
 * only, or it can validate the file using one or more tests.
 * The result of a test is represented as a {@link Validity} state, and these
 * results are combined into a single overall validity for the file.
 * If an individual test fails, then the result for the validator as a whole
 * is generally a failure as well.
 * <p>
 * When a validator fails a file, the audio file as a whole may still pass.
 * This is determined by the {@link Strictness} value for the class.
 * The strictness value may reduce the severity of a validity so that failures
 * are converted to warnings.
 * The factory also determines the strictness of a given validator, using
 * values defined in the <tt>resources/validators.classlist</tt> file
 * The strictness for a given class may be determined using
 * {@link ValidatorFactory#getStrictness(ca.cgjennings.apps.librivox.validators.Validator)}.
 * <p>
 * Individual tests may also have a strictness applied to them, if the
 * validator implementor desires this. The {@link AbstractValidator}
 * includes a facility for fetching and applying a strictness to individual
 * tests which allows the user to control the strictness of tests using
 * configuration files.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public interface Validator {
	/**
	 * Possible results of a validation. A given sound file will be judged one
	 * of <code>FAIL</code>, <code>WARN</code>, or <code>PASS</code>
	 * with respect to a given validation method.
	 */
	public static enum Validity {
		/** The file has failed the validation test. */
		FAIL,
		/**
		 * The file has not failed, but is suspect. A validation test that
		 * produces a high number of false positives should generally not issue
		 * a result worse than this.
		 */
		WARN,
		/**
		 * The file has passed the validation test. A method that is only
		 * meant to provide information should always return this result.
		 */
		PASS,
		/**
		 * This value indicates that the validation rule could not be completed.
		 * Typically, this means that the rule encountered some kind of internal
		 * error or that the rule cannot be applied to files of this type.
		 * In the case of errors, at least, the report should be annotated with
		 * a description of the problem.
		 */
		INCOMPLETE
	};

	/**
	 * This describes how strictly an individual test, or the validity determination
	 * of the entire method, should be applied.
	 */
	public enum Strictness {
		/** If the validation test is failed, the entire file fails. */
		REQUIRED,
		/**
		 * If the validation test is failed, the file can still pass (failures
		 * are converted to warnings).
		 */
		OPTIONAL,
		/**
		 * This validator was disabled by the user. (Validators returned by
		 * {@link #createValidators()} are guaranteed not to be <tt>IGNORE</tt>.)
		 * When this value is used for an individual test, the test result should
		 * be ignored, although
		 */
		IGNORE
	};

	/**
	 * The general category of the validation tests performed by a validator.
	 * This is used to organize and collate the results from different
	 * validators.
	 */
	public enum Category {
		/** For entries related to the file (e.g., file name, size, etc.). */
		FILE,
		/** For entries related to the audio format (number of channels, bitrate, frequency, etc). */
		FORMAT,
		/** For entries related to the actual waveform (loudness, clipping, etc.). */
		AUDIO,
		/** For entries related to the file's metadata (ID3 tags). */
		METADATA,
		/**
		 * For entries that describe recoverable errors.
		 * Note that {@link Validator}s do not normally generate messages in
		 * this category. The main program will generate these messages as
		 * required and pass only data that appears to be valid on to the
		 * <code>Validators</code>.
		 * <p>
		 * There are two levels of error that can occur when analyzing a file.
		 * (Failing a validation test is <b>not</b> an error.)
		 * A "fatal" error prevents the analysis from continuing. An error
		 * message of this kind is noted in the report using {@link #setErrorMessage}.
		 * When a fatal error is set on a report, <b>only</b> that error message,
		 * and any messages set with this category, will be displayed in the report.
		 * Other information posted by a <code>Validator</code>s is ignored.
		 * A "recoverable" error is one that does not prevent the analysis from
		 * proceeding: for example, if a single frame of audio is corrupt but
		 * the rest is OK, that frame can be skipped and analysis can pick up
		 * with the next frame. These are noted in the report whether or not
		 * a fatal error occurs (as they usually indicate corrupt segments of
		 * an audio file).
		 */
		ERROR
	};

	/**
	 * This method will be called to allow the validator to initialize
	 * itself before any analysis is performed.
	 *
	 * @param file the file that will be analyzed by this object
	 * @param report the report that the validator's findings should be
	 *     written to
	 */
	void initialize( LibriVoxAudioFile file, Report report );

	/**
	 * Implementations should return <code>true</code> if they must process
	 * the file's audio samples. A method that checked the file name would
	 * likely return <code>false</code>; one that checked the volume level
	 * would need to return <code>true</code> because it requires sample
	 * data.
	 * <p>
	 * When this method returns <code>true</code>, the validator will call
	 * {@link analyzeFrame} one or more times in sequence to supply audio
	 * samples to this validation method as 16-bit PCM values.
	 *
	 * @return <code>true</code>
	 */
	boolean isAudioProcessor();

	/**
	 * This method is called at the start of validation, after
	 * <code>initialize()</code>, when the analysis process is about to
	 * begin.
	 * 
	 * @param header information about the audio header for the file being processed
	 * @param predecessors validators that have already been initialized; this
	 *     allows the creation of validators that depend in part on results
	 *     generated by other validators
	 */
	void beginAnalysis( AudioHeader header, Validator[] predecessors );

	/**
	 * If {@link #isAudioProcessor} returns <code>true</code>, then this
	 * method will be called after {@link #beginAnalysis} one or more
	 * times as the file's audio samples are decoded. The audio is served
	 * in sequential order, from the start of the track to the end, as a
	 * sequence of {@link AudioFrame} objects. Note that this object,
	 * and/or the sample buffer it contains, may be shared between validators
	 * and between frames. If you must retain information about a frame after
	 * this method returns, you will need to make a copy the information that you
	 * require. You must also work with a copy if you will mutate any of the
	 * data in the <code>AudioFrame</code>
	 *
	 * @param frame a frame of audio samples for the validator to analyze
	 */
	void analyzeFrame( AudioFrame frame );

	/**
	 * Called at the end of validation. This occurs after all the call
	 * to {@link #beginAnalysis} and all calls to {@link #analyzeFrame},
	 * if any.
	 * <p>
	 * After this method returns, the validator is considered to have
	 * finished its validation task. It is not safe to continue writing
	 * to the report or to assume that the audio file given to the
	 * validator will remain valid.
	 * Note that this method might never be called if a severe error
	 * occurs while decoding the audio data.
	 */
	void endAnalysis();

	/**
	 * Return the validity determination for the analyzed file
	 * @return the rating of the file that was determined by the validator
	 */
	Validity getValidity();

	/**
	 * Returns a short, human-friendly name for the validator. This string may
	 * be (and typically is) localized.
	 * 
	 * @return the validator's short name
	 */
	@Override
	String toString();
	
	/**
	 * Returns a brief description of the validator as a human-friendly string.
	 * This string may be (and typically is) localized.
	 * 
	 * @return a description of what the validator tests
	 */
	String getDescription();
}
