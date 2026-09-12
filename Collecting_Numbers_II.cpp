#include <bits/stdc++.h>
using namespace std;

#define f(i, s, e) for (long long i = s; i < e; i++)
#define ll long long
#define pii pair<int, int>
#define pll pair<ll, ll>
#define vi vector<int>
#define vll vector<ll>
#define mii map<int, int>
#define si set<int>
#define sc set<char>
#define ub(hei, num) upper_bound(hei.begin(), hei.end(), num) - hei.begin()
#define lb(hei, num) lower_bound(hei.begin(), hei.end(), num) - hei.begin()

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(NULL);
    int t = 1;
    // cin>>t;
    while (t--)
    {
        ll n, m;
        cin >> n >> m;
        vector<pair<int, int>> temp(n);
        vector<pair<int, int>> remp(n);

        for (int i = 0; i < n; i++)
        {
            cin >> temp[i].first;
            temp[i].second = i;
        }
        remp = temp;
        sort(temp.begin(), temp.end());
        int cnt = 0;
        for (int i = 1; i < temp.size(); i++)
        {
            if (temp[i].second < temp[i - 1].second)
            {
                cnt++;
            }
        }
        for (int i = 0; i < m; i++)
        {
            int a, b;
            cin >> a >> b;
            int in1 = remp[a - 1].first, in2 = remp[b - 1].first;
            int te = 0;
            if (in1 > 0)
            {
                if (temp[in1].second < temp[in1 - 1].second)
                {
                    te--;
                }
                if (temp[in2].second < temp[in2 - 1].second)
                {
                    te++;
                }
            }
            if (in2 > 0)
            {
                if (temp[in2].second < temp[in2 - 1].second)
                {
                    te--;
                }
                if (temp[in1].second < temp[in2 - 1].second)
                {
                    te++;
                }
            }
            swap(temp[in1],temp[in2]);
            cout << cnt + te << endl;
        }
    }

    return 0;
}