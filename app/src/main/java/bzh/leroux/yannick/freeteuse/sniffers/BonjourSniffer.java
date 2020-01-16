package bzh.leroux.yannick.freeteuse.sniffers;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import bzh.leroux.yannick.freeteuse.Freebox;
import bzh.leroux.yannick.freeteuse.Freeteuse;


public class BonjourSniffer extends FreeboxSniffer
{
  private static final String TAG = Freeteuse.TAG + "BonjourSniffer";
  private Context    mContext;
  private NsdManager mNsdManager;

  private NsdManager.DiscoveryListener mDiscoveryListener;
  private NsdManager.ResolveListener   mResolveListener;

  private Handler mHandler;

  public BonjourSniffer (Context                 context,
                         FreeboxSniffer.Listener listener)
  {
    super ("BonjourSniffer",
            listener);

    mContext  = context;
    mHandler  = new Handler ();
  }

  public void start (String serviceType)
  {
    initializeResolveListener   ();
    initializeDiscoveryListener (serviceType);

    mNsdManager = (NsdManager) mContext.getSystemService (Context.NSD_SERVICE);
    if (mNsdManager != null)
    {
      mNsdManager.discoverServices (serviceType,
                                    NsdManager.PROTOCOL_DNS_SD,
                                    mDiscoveryListener);
    }
  }

  public void stop ()
  {
    if (mNsdManager != null)
    {
      try
      {
        mNsdManager.stopServiceDiscovery (mDiscoveryListener);
      }
      catch (IllegalArgumentException ignored)
      {
      }
    }
  }

  private void initializeResolveListener ()
  {
    mResolveListener = new NsdManager.ResolveListener ()
    {
      @Override
      public void onResolveFailed (NsdServiceInfo serviceInfo, int errorCode)
      {
        Log.d (TAG, "Resolve failed");
      }

      @Override
      public void onServiceResolved (final NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service resolved :" + serviceInfo);
        final String hostName = serviceInfo.getHost ().getHostName ();
        final int    port     = serviceInfo.getPort ();

        mHandler.post (new Runnable ()
        {
          @Override
          public void run ()
          {
            Freebox freebox = new Freebox (mContext,
                                           hostName,
                                           port);

            onFreeboxDetected (freebox);
          }
        });
      }
    };
  }

  private void initializeDiscoveryListener (final String serviceType)
  {
    mDiscoveryListener = new NsdManager.DiscoveryListener ()
    {
      @Override
      public void onStartDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Discovery failed");
      }

      @Override
      public void onStopDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Stopping discovery failed");
      }

      @Override
      public void onDiscoveryStarted (String serviceType)
      {
        Log.d (TAG, "Discovery started");
      }

      @Override
      public void onDiscoveryStopped (String serviceType)
      {
        Log.d (TAG, "Discovery stopped");
      }

      @Override
      public void onServiceFound (NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service found: " + serviceInfo.getServiceName ());
        if (serviceInfo.getServiceType ().equals (serviceType + "."))
        {
          mNsdManager.resolveService (serviceInfo,
                                      mResolveListener);
        }
      }

      @Override
      public void onServiceLost (NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service lost: " + serviceInfo.getServiceName ());
      }
    };
  }
}
