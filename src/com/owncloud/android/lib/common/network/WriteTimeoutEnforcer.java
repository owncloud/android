/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2017 ownCloud GmbH.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.network;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Enforces, if possible, a write timeout for a socket.
 *
 * Built as a singleton.
 *
 * Tries to hit something like this:
 * https://android.googlesource.com/platform/external/conscrypt/+/lollipop-release/src/main/java/org/conscrypt/OpenSSLSocketImpl.java#1005
 *
 * Minimizes the chances of getting stalled in PUT/POST request if the network interface is lost while
 * writing the entity into the outwards sockect.
 *
 * It happens. See https://github.com/owncloud/android/issues/1684#issuecomment-295306015
 *
 * @author David A. Velasco
 */
public class WriteTimeoutEnforcer {

    private static final String TAG = WriteTimeoutEnforcer.class.getSimpleName();

    private static final AtomicReference<WriteTimeoutEnforcer> mSingleInstance = new AtomicReference<>();

    private static final String METHOD_NAME = "setSoWriteTimeout";


    private final WeakReference<Class<?>> mSocketClassRef;
    private final WeakReference<Method> mSetSoWriteTimeoutMethodRef;


    /**
     * Private constructor, class is a singleton.
     *
     * @param socketClass               Underlying implementation class of {@link Socket} used to connect
     *                                  with the server.
     * @param setSoWriteTimeoutMethod   Name of the method to call to set a write timeout in the socket.
     */
    private WriteTimeoutEnforcer(Class<?> socketClass, Method setSoWriteTimeoutMethod) {
        mSocketClassRef = new WeakReference<Class<?>>(socketClass);
        mSetSoWriteTimeoutMethodRef =
            (setSoWriteTimeoutMethod == null) ?
                null :
                new WeakReference<>(setSoWriteTimeoutMethod)
        ;
    }


    /**
     * Calls the {@code #setSoWrite(int)} method of the underlying implementation
     * of {@link Socket} if exists.

     * Creates and initializes the single instance of the class when needed
     *
     * @param writeTimeoutMilliseconds  Write timeout to set, in milliseconds.
     * @param socket                    Client socket to connect with the server.
     */
    public static void setSoWriteTimeout(int writeTimeoutMilliseconds, Socket socket) {
        final Method setSoWriteTimeoutMethod = getMethod(socket);
        if (setSoWriteTimeoutMethod != null) {
            try {
                setSoWriteTimeoutMethod.invoke(socket, writeTimeoutMilliseconds);
                Log_OC.i(
                    TAG,
                    "Write timeout set in socket, writeTimeoutMilliseconds: "
                        + writeTimeoutMilliseconds
                );

            } catch (IllegalArgumentException e) {
                Log_OC.e(TAG, "Call to (SocketImpl)#setSoWriteTimeout(int) failed ", e);

            } catch (IllegalAccessException e) {
                Log_OC.e(TAG, "Call to (SocketImpl)#setSoWriteTimeout(int) failed ", e);

            } catch (InvocationTargetException e) {
                Log_OC.e(TAG, "Call to (SocketImpl)#setSoWriteTimeout(int) failed ", e);
            }
        } else {
            Log_OC.i(TAG, "Write timeout for socket not supported");
        }
    }


    /**
     * Gets the method to invoke trying to minimize the cost of reflection reusing objects cached
     * in static members.
     *
     * @param socket    Instance of the socket to use in connection with server.
     * @return          Method to call to set a write timeout in the socket.
     */
    private static Method getMethod(Socket socket) {
        final Class<?> socketClass = socket.getClass();
        final WriteTimeoutEnforcer instance = mSingleInstance.get();
        if (instance == null) {
            return initFrom(socketClass);

        } else if (instance.mSocketClassRef.get() != socketClass) {
            // the underlying class changed
            return initFrom(socketClass);

        } else if (instance.mSetSoWriteTimeoutMethodRef == null) {
            // method not supported
            return null;

        } else {
            final Method cachedSetSoWriteTimeoutMethod = instance.mSetSoWriteTimeoutMethodRef.get();
            return (cachedSetSoWriteTimeoutMethod == null) ?
                initFrom(socketClass) :
                cachedSetSoWriteTimeoutMethod
            ;
        }
    }


    /**
     * Singleton initializer.
     *
     * Uses reflection to extract and 'cache' the method to invoke to set a write timouet in a socket.
     *
     * @param socketClass   Underlying class providing the implementation of {@link Socket}.
     * @return              Method to call to set a write timeout in the socket.
     */
    private static Method initFrom(Class<?> socketClass) {
        Log_OC.i(TAG, "Socket implementation: " + socketClass.getCanonicalName());
        Method setSoWriteTimeoutMethod = null;
        try {
            setSoWriteTimeoutMethod = socketClass.getMethod(METHOD_NAME, int.class);
        } catch (SecurityException e) {
            Log_OC.e(TAG, "Could not access to (SocketImpl)#setSoWriteTimeout(int) method ", e);

        } catch (NoSuchMethodException e) {
            Log_OC.i(
                TAG,
                "Could not find (SocketImpl)#setSoWriteTimeout(int) method - write timeout not supported"
            );
        }
        mSingleInstance.set(new WriteTimeoutEnforcer(socketClass, setSoWriteTimeoutMethod));
        return setSoWriteTimeoutMethod;
    }

}
