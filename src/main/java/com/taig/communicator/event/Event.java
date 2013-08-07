package com.taig.communicator.event;

import com.taig.communicator.request.Request;
import com.taig.communicator.request.Response;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import static com.taig.communicator.concurrent.MainThreadExecutor.EXECUTOR;
import static com.taig.communicator.io.Updateable.*;

/**
 * A collection of callback methods that shall be implemented by subclassing the Event class, overriding the desired
 * methods. Event callbacks are part of a {@link Request} and the overridden methods will
 * be executed during its lifecycle.
 *
 * @param <R> The Request's {@link Response} type.
 */
public abstract class Event<R extends Response>
{
	/**
	 * Retrieve the {@link Event Event's} {@link Proxy}.
	 *
	 * @return The Event's Proxy.
	 */
	public Proxy getProxy()
	{
		return new Proxy();
	}

	/**
	 * Triggers every time the {@link Request Request's} {@link State} changes.
	 *
	 * @param state The Request's current State.
	 */
	protected void onEvent( State state )
	{
		//
	}

	/**
	 * Triggers right after the {@link Request} has been started (with {@link Request#run()} or {@link
	 * Request#request()}) and before a connection to the server is started.
	 */
	protected void onStart()
	{
		//
	}

	/**
	 * Triggers right after the connection has been opened successfully.
	 */
	protected void onConnect()
	{
		//
	}

	/**
	 * Triggers when the {@link Request} has been cancelled by the user.
	 * <p/>
	 * When a Request is cancelled the {@link Event Events} {@link #onFailure(Throwable)} and {@link #onFinish()} will
	 * not be fired.
	 *
	 * @param exception The {@link InterruptedIOException} thrown by cancelling the Request.
	 */
	protected void onCancel( InterruptedIOException exception )
	{
		//
	}

	/**
	 * Triggers every time the {@link Request Request's} {@link OutputStream} sends an {@link Stream.Output#update()}.
	 *
	 * @param current The already received amount of bytes.
	 * @param total   The total amount of bytes to send or <code>-1</code> if unknown.
	 */
	protected void onSend( long current, long total )
	{
		//
	}

	/**
	 * Triggers every time the {@link Request Request's} {@link OutputStream} sends an {@link Stream.Output#update()}.
	 * <p/>
	 * This method depends on {@link #onSend(long, long)} and will only trigger when the <code>total</code> parameter
	 * is <code>> 0</code>.
	 *
	 * @param progress The current upload progress as a value between 0 and 100.
	 */
	protected void onSend( int progress )
	{
		//
	}

	/**
	 * Triggers every time the {@link Request Request's} {@link InputStream} sends an {@link Stream.Input#update()}.
	 * <p/>
	 * This callback relies on the response header <code>Content-Length</code> to provide the total amount of
	 * bytes to be transferred. This header is neither guaranteed to be set (<code>total</code> will be
	 * <code>-1</code>)
	 * nor necessarily correct. Use with caution.
	 *
	 * @param current The already sent amount of bytes.
	 * @param total   The total amount of bytes to receive or <code>-1</code> if unknown.
	 */
	protected void onReceive( long current, long total )
	{
		//
	}

	/**
	 * Triggers every time the {@link Request Request's} {@link InputStream} sends an {@link Stream.Input#update()}.
	 * <p/>
	 * This method depends on {@link #onReceive(long, long)} and will only trigger when the <code>total</code>
	 * parameter is <code>> 0</code>.
	 * <p/>
	 * This callback relies on the response header <code>Content-Length</code> to provide the total amount of
	 * bytes to be transferred. This value is not necessarily correct. Use with caution.
	 *
	 * @param progress The current download progress as a value between 0 and 100.
	 */
	protected void onReceive( int progress )
	{
		//
	}

	/**
	 * Triggers when the {@link Request} has completed successfully and the response has already been parsed.
	 *
	 * @param response The Request's {@link Response} ({@link R}).
	 */
	protected void onSuccess( R response )
	{
		//
	}

	/**
	 * Triggers when the {@link Request} has failed for some reason.
	 * <p/>
	 * This callback does not trigger when the Request has been cancelled.
	 *
	 * @param error The {@link Throwable} that caused the Request to fail.
	 * @see #onCancel(InterruptedIOException)
	 */
	protected void onFailure( Throwable error )
	{
		//
	}

	/**
	 * Triggers when the {@link Request} has finished either after {@link #onSuccess(Response)} or {@link
	 * #onFailure(Throwable)}.
	 */
	protected void onFinish()
	{
		//
	}

	public static abstract class Payload<T> extends Event<Response.Payload<T>>
	{
		@Override
		public Proxy getProxy()
		{
			return new Proxy();
		}

		protected void onSuccess( T payload ) {}

		public class Proxy extends Event<Response.Payload<T>>.Proxy
		{
			@Override
			public void success( final Response.Payload<T> response )
			{
				EXECUTOR.execute( new Runnable()
				{
					@Override
					public void run()
					{
						onEvent( State.SUCCESS );
						onSuccess( response );
						onSuccess( response.getPayload() );
						onFinish();
					}
				} );
			}
		}
	}

	public class Proxy
	{
		public Event<R> getEvent()
		{
			return Event.this;
		}

		public void start()
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.START );
					onStart();
				}
			} );
		}

		public void connect()
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.CONNECT );
					onConnect();
				}
			} );
		}

		public void cancel( final InterruptedIOException exception )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.CANCEL );
					onCancel( exception );
				}
			} );
		}

		public void send( final int current, final long total )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					// TODO only trigger once
					onEvent( State.SEND );
					onSend( current, total );

					if( total > 0 )
					{
						onSend( (int) ( current / (float) total * 100 ) + 1 );
					}
				}
			} );
		}

		public void receive( final int current, final long total )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					// TODO only trigger once
					onEvent( State.RECEIVE );
					onReceive( current, total );

					if( total > 0 )
					{
						onReceive( (int) ( current / (float) total * 100 ) + 1 );
					}
				}
			} );
		}

		public void success( final R response )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.SUCCESS );
					onSuccess( response );
					onFinish();
				}
			} );
		}

		public void failure( final Throwable error )
		{
			EXECUTOR.execute( new Runnable()
			{
				@Override
				public void run()
				{
					onEvent( State.FAILURE );
					onFailure( error );
					onFinish();
				}
			} );
		}
	}
}