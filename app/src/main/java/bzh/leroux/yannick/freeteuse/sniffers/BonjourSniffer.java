package bzh.leroux.yannick.freeteuse.sniffers;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import bzh.leroux.yannick.freeteuse.Freebox;
import bzh.leroux.yannick.freeteuse.Freeteuse;

public class BonjourSniffer extends FreeboxSniffer
{
  private static final String TAG = Freeteuse.TAG + "BonjourSniffer";

  private ResolverQueue                      mResolverQueue;
  private NsdManager                         mNsdManager;
  private List<NsdManager.DiscoveryListener> mDiscoveryListeners;
  private int                                mDefaultPort;

  // ---------------------------------------------------
  private class ResolverQueue
  {
    private Queue<NsdServiceInfo> mQueue;
    private boolean               mInProgress;
    private NsdManager            mNsdManager;

    ResolverQueue (NsdManager manager)
    {
      mQueue      = new LinkedList<> ();
      mNsdManager = manager;
    }

    void add (NsdServiceInfo serviceInfo)
    {
      mQueue.add (serviceInfo);
      resolveNext ();
    }

    void resolveNext ()
    {
      if (!(mInProgress || mQueue.isEmpty ()))
      {
        mInProgress = true;
        mNsdManager.resolveService (mQueue.poll (),
                                    new BonjourResolveListener ());
      }
    }

    void resolutionReceived ()
    {
      mInProgress = false;
      resolveNext ();
    }
  }

  // ---------------------------------------------------
  private class BonjourResolveListener implements NsdManager.ResolveListener
  {
    @Override
    public void onResolveFailed (NsdServiceInfo serviceInfo,
                                 int            errorCode)
    {
      Log.e (TAG, "Resolve failed (" + errorCode + ")");
      mResolverQueue.resolutionReceived ();
    }

    @Override
    public void onServiceResolved (final NsdServiceInfo serviceInfo) {
      String serviceName = serviceInfo.getServiceName ();

      Log.d (TAG, "Service resolved :" + serviceInfo);

      if ((serviceName != null) && serviceName.contains ("Freebox Player"))
      {
        Freebox freebox;
        int     port = serviceInfo.getPort ();

        if (port == 0) {
          port = mDefaultPort;
        }

        freebox = new Freebox (serviceInfo.getHost ().getHostAddress (),
                               port,
                               serviceInfo.toString ());

        onFreeboxDetected (freebox);
      }

      mResolverQueue.resolutionReceived ();
    }
  }

  // ---------------------------------------------------
  public BonjourSniffer (Context                 context,
                         FreeboxSniffer.Listener listener,
                         int                     default_port)
  {
    super (TAG,
            listener);

    mDefaultPort        = default_port;
    mDiscoveryListeners = new ArrayList<> ();

    mNsdManager = (NsdManager) context.getSystemService (Context.NSD_SERVICE);
    if (mNsdManager != null)
    {
      mResolverQueue = new ResolverQueue (mNsdManager);
    }
  }

  // ---------------------------------------------------
  public void start (String serviceType)
  {
    if (mNsdManager != null)
    {
      NsdManager.DiscoveryListener discoveryListener = getDiscoveryListener (serviceType);

      mDiscoveryListeners.add (discoveryListener);

      mNsdManager.discoverServices (serviceType,
                                    NsdManager.PROTOCOL_DNS_SD,
                                    discoveryListener);
    }
  }

  // ---------------------------------------------------
  public void stop ()
  {
    if (mNsdManager != null)
    {
      try
      {
        for (NsdManager.DiscoveryListener listener: mDiscoveryListeners)
        {
          mNsdManager.stopServiceDiscovery (listener);
        }
      }
      catch (IllegalArgumentException ignored)
      {
      }
    }
  }

  // ---------------------------------------------------
  private NsdManager.DiscoveryListener getDiscoveryListener (final String serviceType)
  {
    return new NsdManager.DiscoveryListener ()
    {
      @Override
      public void onStartDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Discovery failed (" + serviceType +")");
      }

      @Override
      public void onStopDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Stopping discovery failed (" + serviceType +")");
      }

      @Override
      public void onDiscoveryStarted (String serviceType)
      {
        Log.d (TAG, "Discovery started (" + serviceType +")");
      }

      @Override
      public void onDiscoveryStopped (String serviceType)
      {
        Log.d (TAG, "Discovery stopped (" + serviceType +")");
      }

      @Override
      public void onServiceFound (NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service found: << " + serviceInfo.getServiceName () + " >>");
        if (serviceInfo.getServiceType ().startsWith (serviceType))
        {
          mResolverQueue.add (serviceInfo);
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
